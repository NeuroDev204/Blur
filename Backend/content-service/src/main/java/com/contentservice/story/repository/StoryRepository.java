package com.contentservice.story.repository;

import com.contentservice.story.entity.Story;
import com.contentservice.story.entity.StoryLike;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StoryRepository extends Neo4jRepository<Story, String> {

	@Query("MATCH (s:story {authorId: $userId}) DETACH DELETE s")
	void deleteByAuthorId(@Param("userId") String userId);

	@Query("""
			MATCH (u:user_profile {user_id: $userId}), (s:story {id: $storyId})
			MERGE (u)-[:CREATED_STORY {createdAt: $createdAt}]->(s)
			""")
	void linkStoryToAuthor(
			@Param("userId") String userId,
			@Param("storyId") String storyId,
			@Param("createdAt") Instant createdAt);

	/**
	 * Traverse (user_profile)-[:CREATED_STORY]->(story) to find stories by author.
	 */
	@Query("""
			MATCH (u:user_profile {user_id: $authorId})-[:CREATED_STORY]->(s:story)
			RETURN s ORDER BY s.createdAt DESC
			""")
	List<Story> findAllByAuthorId(@Param("authorId") String authorId);

	/**
	 * Find stories older than a given timestamp (for the scheduled 24h cleanup
	 * job).
	 */
	@Query("""
			MATCH (s:story)
			WHERE s.createdAt < $timestamp
			RETURN s
			""")
	List<Story> findAllByCreatedAtBefore(@Param("timestamp") Instant timestamp);

	// ─────────────────────────────────────────────────────────────
	// LIKE RELATIONSHIP
	// (user_profile)-[:LIKED_STORY {createdAt}]->(story)
	// ─────────────────────────────────────────────────────────────

	/** Create the LIKED_STORY edge (idempotent via MERGE). */
	@Query("""
			MATCH (u:user_profile {user_id: $userId}), (s:story {id: $storyId})
			MERGE (u)-[r:LIKED_STORY]->(s)
			ON CREATE SET r.createdAt = $createdAt
			""")
	void likeStory(
			@Param("userId") String userId,
			@Param("storyId") String storyId,
			@Param("createdAt") Instant createdAt);

	/** Remove the LIKED_STORY edge. */
	@Query("""
			MATCH (u:user_profile {user_id: $userId})-[r:LIKED_STORY]->(s:story {id: $storyId})
			DELETE r
			""")
	void unlikeStory(@Param("userId") String userId, @Param("storyId") String storyId);

	/** Return all likes on a story as @QueryResult StoryLike DTOs. */
	@Query("""
			MATCH (u:user_profile)-[r:LIKED_STORY]->(s:story {id: $storyId})
			RETURN u.user_id AS userId, r.createdAt AS createdAt
			""")
	List<StoryLike> findLikesByStoryId(@Param("storyId") String storyId);
}
