package com.blur.profileservice.repository;

import com.blur.profileservice.entity.UserProfile;
import feign.Param;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends Neo4jRepository<UserProfile, String> {
    Optional<UserProfile> findUserProfileByUserId(String userId);

    Optional<UserProfile> findUserProfileById(String id);

    @Query("MATCH (u:user_profile)-[:follows]->(f:user_profile) WHERE u.id = $id RETURN f")
    List<UserProfile> findAllFollowingById(String id);

    @Query("MATCH (f:user_profile)-[:follows]->(u:user_profile) WHERE u.id = $id RETURN f")
    List<UserProfile> findAllFollowersById(@Param("id") String id);

    Optional<UserProfile> findByUserId(String userId);

    List<UserProfile> findAllByFirstNameContainingIgnoreCase(String firstName);

    @Query("""
            MATCH (a:user_profile {id: $fromId})
            MATCH (b:user_profile {id: $toId})
            MERGE (a)-[:follows]->(b)
            """)
    void follow(@Param("fromId") String fromId, @Param("toId") String toId);

    @Query("""
            MATCH (a:user_profile {id: $fromId})-[r:follows]->(b:user_profile {id: $toId})
            DELETE r
            """)
    void unfollow(@Param("fromId") String fromId, @Param("toId") String toId);

    List<UserProfile> findAllByUsernameLike(String username);

}
