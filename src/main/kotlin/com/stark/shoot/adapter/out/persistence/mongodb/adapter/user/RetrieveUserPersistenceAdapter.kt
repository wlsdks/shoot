package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.UserMongoRepository
import com.stark.shoot.application.port.out.user.RetrieveUserPort
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
class RetrieveUserPersistenceAdapter(
    private val userMongoRepository: UserMongoRepository,
    private val userMapper: UserMapper,
    private val mongoTemplate: MongoTemplate
) : RetrieveUserPort {

    /**
     * 사용자명으로 사용자 조회
     */
    override fun findByUsername(
        username: String
    ): User? {
        val userDocument = userMongoRepository.findByUsername(username)
        return userDocument?.let { userMapper.toDomain(it) }
    }

    /**
     * ID로 사용자 조회
     */
    override fun findById(
        id: ObjectId
    ): User? {
        val userDocument = userMongoRepository.findById(id)
        return if (userDocument.isPresent) {
            userMapper.toDomain(userDocument.get())
        } else null
    }

    /**
     * 모든 사용자 조회
     */
    override fun findAll(): List<User> {
        val docs = userMongoRepository.findAll()
        return docs.map(userMapper::toDomain)
    }

    /**
     * 사용자 코드로 사용자 조회
     */
    override fun findByUserCode(
        userCode: String
    ): User? {
        val doc = userMongoRepository.findByUserCode(userCode) ?: return null
        return userMapper.toDomain(doc)
    }

    /**
     * 랜덤 사용자 조회
     */
    override fun findRandomUsers(
        excludeUserId: ObjectId,
        limit: Int
    ): List<User> {
        // 1) match _id != excludeUserId
        val matchStage = Aggregation.match(Criteria.where("_id").ne(excludeUserId))
        // 2) sample limit
        val sampleStage = Aggregation.sample(limit.toLong())
        // 3) pipeline
        val pipeline = Aggregation.newAggregation(matchStage, sampleStage)

        val results = mongoTemplate.aggregate(pipeline, "users", UserDocument::class.java)
        val docs = results.mappedResults
        return docs.map(userMapper::toDomain)
    }

    /**
     * 코드로 사용자 조회
     */
    override fun findByCode(
        newCode: String
    ): User? {
        val doc = userMongoRepository.findByUserCode(newCode) ?: return null
        return userMapper.toDomain(doc)
    }

    /**
     * BFS 기반 친구 추천 Aggregation 파이프라인 실행 메서드.
     *
     * 단계별 설명:
     * 1. 시작 사용자 선택: 입력받은 userId와 일치하는 사용자(A)를 조회한다.
     * 2. 친구 네트워크 탐색: A의 friends 배열을 시작점으로 하여 $graphLookup를 통해 최대 maxDepth 단계까지의 친구들을 조회한다.
     *    예) A의 직접 친구(B, C)와 이들의 친구들(D, E, F, G)을 조회.
     * 3. 추천 제외 대상 계산: A의 friends, incomingFriendRequests, outgoingFriendRequests, 그리고 A 자신을 exclusions 배열로 만든다.
     * 4. 후보 분리: 조회된 네트워크 배열을 unwind하여 각 후보 문서를 개별적으로 처리할 수 있게 한다.
     * 5. 문서 재구조화: 각 후보 문서에 candidate(후보 사용자 정보), exclusions, startUserFriends(시작 사용자의 친구 목록)을 포함시킨다.
     * 6. 후보 필터링: candidate의 _id가 exclusions 배열에 포함되어 있으면(즉, 이미 친구이거나 본인일 경우) 후보에서 제외한다.
     * 7. 상호 친구 수 계산: 각 후보와 A의 친구 목록의 교집합 크기를 계산해 mutualCount 필드를 추가한다.
     * 8. 정렬 및 제한: mutualCount 기준 내림차순 정렬 후 limit 개수만큼의 추천 결과를 선택한다.
     * 9. 최종 문서 구성: 최종 결과 문서를 candidate 필드의 값으로 대체한다.
     *
     * @param userId 추천 대상 사용자의 ObjectId
     * @param maxDepth 친구 네트워크 탐색 최대 깊이 (예: 2)
     * @param limit 반환할 최대 추천 사용자 수
     * @return 추천된 사용자 목록
     */
    override fun findBFSRecommendedUsers(
        userId: ObjectId,
        maxDepth: Int,
        limit: Int
    ): List<User> {
        // 1. 시작 사용자 매칭 (_id == userId)
        val matchStage = Aggregation.match(Criteria.where("_id").`is`(userId))

        // 2. $graphLookup: 시작 사용자의 friends를 시작점으로 최대 maxDepth까지 탐색
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

        // 10. limit 적용
        val limitStage = Aggregation.limit(limit.toLong())

        // 11. 최종적으로 candidate 필드만으로 문서를 대체
        // "$candidate" 라는 표현은 candidate 필드의 값을 의미합니다.
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
            limitStage,
            finalReplaceWithStage
        )

        val results = mongoTemplate.aggregate(aggregation, "users", UserDocument::class.java)
        return results.mappedResults.map { userMapper.toDomain(it) }
    }

}