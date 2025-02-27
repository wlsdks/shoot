package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.application.port.out.user.RecommendFriendPort
import com.stark.shoot.domain.chat.user.User
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.GraphLookupOperation
import org.springframework.data.mongodb.core.aggregation.ReplaceWithOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component

@Component
class RecommendFriendPersistenceAdapter(
    private val userMapper: UserMapper,
    private val mongoTemplate: MongoTemplate
) : RecommendFriendPort {

    /**
     * BFS 기반 친구 추천 Aggregation 파이프라인 실행 메서드.
     * @param userId 추천 대상 사용자의 ObjectId
     * @param maxDepth 친구 네트워크 탐색 최대 깊이 (예: 2)
     * @param skip 이미 반환한 결과 수 (페이지네이션)
     * @param limit 반환할 최대 추천 사용자 수
     * @return 추천된 사용자 목록
     */
    override fun findBFSRecommendedUsers(
        userId: ObjectId,
        maxDepth: Int,
        skip: Int,
        limit: Int
    ): List<User> {
        // 1. 시작 사용자 매칭 (_id == userId)
        val matchStage = Aggregation.match(Criteria.where("_id").`is`(userId))

        // 2. $graphLookup: 시작 사용자의 friends 배열을 시작점으로 최대 maxDepth까지 탐색
        val graphLookupStage = GraphLookupOperation.builder()
            .from("users")
            .startWith("\$friends")
            .connectFrom("friends")
            .connectTo("_id")
            .maxDepth(maxDepth.toLong())
            .depthField("depth")
            .`as`("network")

        // 3. exclusions 계산: 시작 사용자의 friends, incomingFriendRequests, outgoingFriendRequests, 자기 자신
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

        // 4. 필요한 필드만 project (network, exclusions, startUserFriends)
        val projectStage = Aggregation.project("network", "exclusions", "startUserFriends")

        // 5. network 배열을 unwind하여 각 친구 문서로 분리
        val unwindStage = Aggregation.unwind("network")

        // 6. $replaceWith: network 필드를 candidate로 포함하고, exclusions와 startUserFriends도 함께 유지
        val replaceWithStage = ReplaceWithOperation.replaceWithValue(
            Document().apply {
                put("candidate", "\$network")
                put("exclusions", "\$exclusions")
                put("startUserFriends", "\$startUserFriends")
            }
        )

        // 7. candidate의 _id가 exclusions 배열에 포함되지 않은 문서만 선택 ($expr 사용)
        val matchExclusionsStage = Aggregation.match(
            Criteria.where("\$expr").`is`(
                Document("\$not", Document("\$in", listOf("\$candidate._id", "\$exclusions")))
            )
        )

        // 8. candidate와 시작 사용자의 friends의 교집합 크기를 계산하여 mutualCount 필드 추가
        val addMutualCountStage = AddFieldsOperation.builder()
            .addField("mutualCount")
            .withValue(
                Document("\$size", Document("\$setIntersection", listOf("\$candidate.friends", "\$startUserFriends")))
            )
            .build()

        // 9. mutualCount 내림차순 정렬
        val sortStage = Aggregation.sort(Sort.by(Sort.Direction.DESC, "mutualCount"))

        // 10. skip 및 limit 적용 (페이지네이션)
        val skipStage = Aggregation.skip(skip.toLong())
        val limitStage = Aggregation.limit(limit.toLong())

        // 11. 최종적으로 candidate 필드만으로 문서를 대체
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
            sortStage,
            skipStage,
            limitStage,
            finalReplaceWithStage
        )

        val results = mongoTemplate.aggregate(aggregation, "users", UserDocument::class.java)
        return results.mappedResults.map { userMapper.toDomain(it) }
    }

}
