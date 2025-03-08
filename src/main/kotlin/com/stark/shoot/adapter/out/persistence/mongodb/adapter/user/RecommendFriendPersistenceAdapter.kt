package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.application.port.out.user.friend.RecommendFriendPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.Adapter
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.GraphLookupOperation
import org.springframework.data.mongodb.core.aggregation.ReplaceWithOperation
import org.springframework.data.mongodb.core.query.Criteria

@Adapter
class RecommendFriendPersistenceAdapter(
    private val userMapper: UserMapper,
    private val mongoTemplate: MongoTemplate
) : RecommendFriendPort {

    /**
     * BFS 기반 친구 추천 Aggregation 파이프라인 실행 메서드.
     *
     * 단계별 설명:
     * 1. 시작 사용자 선택: 입력받은 userId와 일치하는 사용자(A)를 조회한다.
     * 2. 친구 네트워크 탐색: A의 friends 배열을 시작점으로 하여 $graphLookup를 통해 최대 maxDepth 단계까지의 친구들을 조회한다.
     * 3. 추천 제외 대상 계산: A의 friends, incomingFriendRequests, outgoingFriendRequests, 그리고 A 자신을 exclusions 배열로 만든다.
     * 4. 후보 분리: 조회된 네트워크 배열을 unwind하여 각 후보 문서를 개별적으로 처리할 수 있게 한다.
     * 5. 문서 재구조화: 각 후보 문서에 candidate(후보 사용자 정보), exclusions, startUserFriends(시작 사용자의 친구 목록)을 포함시킨다.
     * 6. 후보 필터링: candidate의 _id가 exclusions 배열에 포함되어 있으면(즉, 이미 친구이거나 본인일 경우) 후보에서 제외한다.
     * 7. 상호 친구 수 계산: 각 후보와 A의 친구 목록의 교집합 크기를 계산해 mutualCount 필드를 추가한다.
     * 8. 정렬 및 페이지네이션: mutualCount 기준 내림차순 정렬 후, skip과 limit을 적용한다.
     * 9. 최종 문서 구성: 최종 결과 문서를 candidate 필드의 값으로 대체한다.
     *
     * 만약 Aggregation 결과가 비어 있다면, fallback으로 랜덤 유저(limit 수 만큼)를 반환한다.
     *
     * @param userId 추천 대상 사용자의 ObjectId
     * @param maxDepth 친구 네트워크 탐색 최대 깊이 (예: 2)
     * @param skip 이미 반환한 결과 수 (페이지네이션)
     * @param limit 반환할 최대 추천 사용자 수
     * @return 추천된 사용자 목록 (추천 결과가 없으면 랜덤 유저 목록)
     */
    override fun findBFSRecommendedUsers(
        userId: ObjectId,
        maxDepth: Int,
        skip: Int,
        limit: Int
    ): List<User> {
        // 1. 시작 사용자 매칭
        val matchStage = Aggregation.match(Criteria.where("_id").`is`(userId))

        // 2. $graphLookup: 친구 네트워크 탐색
        val graphLookupStage = GraphLookupOperation.builder()
            .from("users")
            .startWith("\$friends")
            .connectFrom("friends")
            .connectTo("_id")
            .maxDepth(maxDepth.toLong())
            .depthField("depth")
            .`as`("network")

        // 3. exclusions 계산: 친구, 요청, 자기 자신
        val addExclusionsStage = AddFieldsOperation.builder()
            .addField("exclusions")
            .withValue(
                Document(
                    "\$setUnion", listOf(
                        "\$friends",
                        "\$incomingFriendRequests",
                        "\$outgoingFriendRequests",
                        listOf("\$_id")
                    )
                )
            )
            .addField("startUserFriends")
            .withValue("\$friends")
            .build()

        // 4. 필요한 필드만 프로젝션
        val projectStage = Aggregation.project("network", "exclusions", "startUserFriends")

        // 5. network 배열을 unwind하여 각 친구 문서로 분리
        val unwindStage = Aggregation.unwind("network")

        // 6. candidate 필드 재구조화
        val replaceWithStage = ReplaceWithOperation.replaceWithValue(
            Document().apply {
                put("candidate", "\$network")
                put("exclusions", "\$exclusions")
                put("startUserFriends", "\$startUserFriends")
            }
        )

        // 7. exclusions 기준 필터링 (이미 친구, 요청자, 자기 자신 제외)
        val matchExclusionsStage = Aggregation.match(
            Criteria.where("\$expr").`is`(
                Document("\$not", Document("\$in", listOf("\$candidate._id", "\$exclusions")))
            )
        )

        // 8. 상호 친구 수 계산
        val addMutualCountStage = AddFieldsOperation.builder()
            .addField("mutualCount")
            .withValue(
                Document("\$size", Document("\$setIntersection", listOf("\$candidate.friends", "\$startUserFriends")))
            )
            .build()

        // 9. 그룹화 단계: candidate._id 기준 중복 제거
        val groupStage = Aggregation.group("\$candidate._id")
            .first("candidate").`as`("candidate")
            .max("mutualCount").`as`("mutualCount")

        // 10. mutualCount 내림차순 정렬
        val sortStage = Aggregation.sort(Sort.by(Sort.Direction.DESC, "mutualCount"))

        // 11. 페이지네이션: skip, limit
        val skipStage = Aggregation.skip(skip.toLong())
        val limitStage = Aggregation.limit(limit.toLong())

        // 12. 최종적으로 candidate 필드만 반환
        val finalReplaceWithStage = ReplaceWithOperation.replaceWithValue("\$candidate")

        // Aggregation 파이프라인 구성
        val aggregation = Aggregation.newAggregation(
            matchStage,
            graphLookupStage,
            addExclusionsStage,
            projectStage,
            unwindStage,
            replaceWithStage,
            matchExclusionsStage,
            addMutualCountStage,
            groupStage, // 추가된 그룹 단계로 중복 제거
            sortStage,
            skipStage,
            limitStage,
            finalReplaceWithStage
        )

        // BFS 추천 결과 실행
        val results = mongoTemplate.aggregate(aggregation, "users", UserDocument::class.java)
        val recommendedUsers = results.mappedResults.map { userMapper.toDomain(it) }

        // fallback: 추천 결과가 없을 경우 자기 자신 제외 후 랜덤 반환
        if (recommendedUsers.isEmpty()) {
            val fallbackCriteria = Criteria.where("_id").ne(userId)
            val fallbackAggregation = Aggregation.newAggregation(
                Aggregation.match(fallbackCriteria),
                Aggregation.sample(limit.toLong())
            )
            val fallbackResults = mongoTemplate.aggregate(fallbackAggregation, "users", UserDocument::class.java)
            return fallbackResults.mappedResults.map { userMapper.toDomain(it) }
        }
        return recommendedUsers
    }

}
