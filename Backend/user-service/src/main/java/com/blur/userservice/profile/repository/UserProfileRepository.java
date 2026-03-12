package com.blur.userservice.profile.repository;

import com.blur.userservice.profile.entity.UserProfile;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends Neo4jRepository<UserProfile, String> {

  @Query("""
      MATCH (follower:user_profile)-[:follows]->(u:user_profile {user_id: $userId})
      RETURN follower.user_id
      """)
  List<String> findFollowerUserIdsByUserId(@Param("userId") String userId);

  @Query("MATCH (u:user_profile {user_id: $userId}) DETACH DELETE u")
  void deleteByUserId(@Param("userId") String userId);

  // Override default methods to prevent infinite recursion on self-referencing
  // @Relationship
  @Query("MATCH (u:user_profile {user_id: $userId}) RETURN u")
  Optional<UserProfile> findUserProfileByUserId(String userId);

  @Query("MATCH (u:user_profile) WHERE u.id = $id RETURN u")
  Optional<UserProfile> findUserProfileById(String id);

  @Query("MATCH (u:user_profile {user_id: $userId}) RETURN u")
  Optional<UserProfile> findByUserId(String userId);

  @Query("MATCH (u:user_profile) WHERE toLower(u.username) = toLower($username) RETURN u")
  Optional<UserProfile> findByUsername(String username);

  @Query("MATCH (u:user_profile) WHERE toLower(u.email) = toLower($email) RETURN u")
  Optional<UserProfile> findByEmail(String email);

  @Query("MATCH (u:user_profile)-[:follows]->(f:user_profile) WHERE u.id = $id RETURN f")
  List<UserProfile> findAllFollowingById(String id);

  @Query("MATCH (f:user_profile)-[:follows]->(u:user_profile) WHERE u.id = $id RETURN f")
  List<UserProfile> findAllFollowersById(@Param("id") String id);

  @Query("MATCH (u:user_profile) WHERE toLower(u.firstName) CONTAINS toLower($firstName) RETURN u")
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

  @Query("MATCH (u:user_profile) WHERE u.username CONTAINS $username RETURN u")
  List<UserProfile> findAllByUsernameLike(String username);

  // follower chung (ban cua ban be)
  @Query("""
          MATCH (me:user_profile {id: $userId})-[:follows]->(myFollowing:user_profile)
          MATCH (myFollowing)-[:follows]->(recommended:user_profile)
          WHERE recommended <> me
            AND NOT (me)-[:follows]->(recommended)
            AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
            AND NOT EXISTS((recommended)-[:BLOCKED]->(me))
          WITH recommended, COUNT(DISTINCT myFollowing) AS mutualCount
          ORDER BY mutualCount DESC
          SKIP $skip
          LIMIT $limit
          RETURN recommended
      """)
  List<UserProfile> findMutualRecommendations(
      @Param("userId") String userId,
      @Param("skip") int skip,
      @Param("limit") int limit);

  // dem so goi y dua tren ket noi chung
  @Query("""
          MATCH (me:user_profile {id: $userId})-[:follows]->(myFollowing:user_profile)
          MATCH (myFollowing)-[:follows]->(recommended:user_profile)
          WHERE recommended <> me
            AND NOT (me)-[:follows]->(recommended)
            AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
          RETURN COUNT(DISTINCT recommended)
      """)
  long countMutualRecommendations(@Param("userId") String userId);

  @Query("""
          MATCH (me:user_profile {id: $userId})-[:follows]->(myFollowing:user_profile)
          MATCH (myFollowing)-[:follows]->(recommended:user_profile)
          WHERE recommended <> me
            AND NOT (me)-[:follows]->(recommended)
            AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
          WITH recommended,
               COUNT(DISTINCT myFollowing) AS mutualCount,
               COLLECT(DISTINCT myFollowing.firstName + ' ' + myFollowing.lastName)[0..3] AS mutualNames
          ORDER BY mutualCount DESC
          SKIP $skip
          LIMIT $limit
          RETURN recommended.id AS id,
                 recommended.userId AS userId,
                 recommended.username AS username,
                 recommended.firstName AS firstName,
                 recommended.lastName AS lastName,
                 recommended.imageUrl AS imageUrl,
                 recommended.bio AS bio,
                 recommended.city AS city,
                 recommended.followersCount AS followersCount,
                 recommended.followingCount AS followingCount,
                 mutualCount AS mutualConnections,
                 mutualNames AS mutualNames
      """)
  List<MutualRecommendationProjection> findMutualRecommendationsWithDetails(
      @Param("userId") String userId,
      @Param("skip") int skip,
      @Param("limit") int limit);

  // tim nguoi co so thich tuong tu (follow cung nguoi)
  @Query("""
          MATCH (me:user_profile {id: $userId})-[:follows]->(following:user_profile)
          MATCH (recommended:user_profile)-[:follows]->(following)
          WHERE recommended <> me
            AND NOT (me)-[:follows]->(recommended)
            AND NOT (recommended)-[:follows]->(me)
            AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
          WITH recommended, COUNT(DISTINCT following) AS sharedCount
          ORDER BY sharedCount DESC, recommended.followersCount DESC
          SKIP $skip
          LIMIT $limit
          RETURN recommended
      """)
  List<UserProfile> findSimilarTasteRecommendations(
      @Param("userId") String userId,
      @Param("skip") int skip,
      @Param("limit") int limit);

  // dem so goi y dua tren so thich tuong tu
  @Query("""
          MATCH (me:user_profile {id: $userId})-[:follows]->(following:user_profile)
          MATCH (recommended:user_profile)-[:follows]->(following)
          WHERE recommended <> me
            AND NOT (me)-[:follows]->(recommended)
            AND NOT (recommended)-[:follows]->(me)
          RETURN COUNT(DISTINCT recommended)
      """)
  long countSimilarTasteRecommendations(@Param("userId") String userId);

  // tim nguoi co thuoc tinh chung (chung thanh pho)
  @Query("""
          MATCH (me:user_profile {id: $userId})
          WHERE me.city IS NOT NULL AND me.city <> ''
          MATCH (recommended:user_profile)
          WHERE recommended.city = me.city
            AND recommended <> me
            AND NOT (me)-[:follows]->(recommended)
            AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
          RETURN recommended
          ORDER BY recommended.followersCount DESC, recommended.updatedAt DESC
          SKIP $skip
          LIMIT $limit
      """)
  List<UserProfile> findSameCityRecommendations(
      @Param("userId") String userId,
      @Param("skip") int skip,
      @Param("limit") int limit);

  /**
   * Đếm tổng số người dùng cùng thành phố
   */
  @Query("""
          MATCH (me:user_profile {id: $userId})
          WHERE me.city IS NOT NULL AND me.city <> ''
          MATCH (recommended:user_profile)
          WHERE recommended.city = me.city
            AND recommended <> me
            AND NOT (me)-[:follows]->(recommended)
          RETURN COUNT(DISTINCT recommended)
      """)
  long countSameCityRecommendations(@Param("userId") String userId);

  /**
   * Tìm người dùng phổ biến và hoạt động gần đây
   */
  @Query("""
          MATCH (me:user_profile {id: $userId})
          MATCH (recommended:user_profile)
          WHERE recommended <> me
            AND NOT (me)-[:follows]->(recommended)
            AND NOT EXISTS((me)-[:BLOCKED]->(recommended))
            AND recommended.followersCount >= $minFollowers
          RETURN recommended
          ORDER BY recommended.followersCount DESC, recommended.updatedAt DESC
          SKIP $skip
          LIMIT $limit
      """)
  List<UserProfile> findPopularRecommendations(
      @Param("userId") String userId,
      @Param("minFollowers") int minFollowers,
      @Param("skip") int skip,
      @Param("limit") int limit);

  /**
   * Đếm người dùng phổ biến
   */
  @Query("""
          MATCH (me:user_profile {id: $userId})
          MATCH (recommended:user_profile)
          WHERE recommended <> me
            AND NOT (me)-[:follows]->(recommended)
            AND recommended.followersCount >= $minFollowers
          RETURN COUNT(DISTINCT recommended)
      """)
  long countPopularRecommendations(
      @Param("userId") String userId,
      @Param("minFollowers") int minFollowers);

  /**
   * Gợi ý kết hợp đa yếu tố với trọng số
   */
  @Query("""
          MATCH (me:user_profile {id: $userId})

          // Yếu tố 1: Kết nối chung (weight = 3)
          OPTIONAL MATCH (me)-[:follows]->(myFollowing:user_profile)-[:follows]->(mutualRec:user_profile)
          WHERE mutualRec <> me
            AND NOT (me)-[:follows]->(mutualRec)
            AND NOT EXISTS((me)-[:BLOCKED]->(mutualRec))

          // Yếu tố 2: Cùng thành phố (weight = 2)
          OPTIONAL MATCH (cityRec:user_profile)
          WHERE cityRec.city = me.city
            AND cityRec <> me
            AND NOT (me)-[:follows]->(cityRec)
            AND NOT EXISTS((me)-[:BLOCKED]->(cityRec))

          // Yếu tố 3: Sở thích tương tự (weight = 2)
          OPTIONAL MATCH (me)-[:follows]->(following:user_profile)<-[:follows]-(tasteRec:user_profile)
          WHERE tasteRec <> me
            AND NOT (me)-[:follows]->(tasteRec)
            AND NOT EXISTS((me)-[:BLOCKED]->(tasteRec))

          WITH me,
               COLLECT(DISTINCT mutualRec) AS mutualRecs,
               COLLECT(DISTINCT cityRec) AS cityRecs,
               COLLECT(DISTINCT tasteRec) AS tasteRecs

          // Kết hợp tất cả
          WITH mutualRecs + cityRecs + tasteRecs AS allRecs, mutualRecs, cityRecs, tasteRecs
          UNWIND allRecs AS rec
          WHERE rec IS NOT NULL

          // Tính điểm
          WITH rec,
               CASE WHEN rec IN mutualRecs THEN 3 ELSE 0 END AS mutualScore,
               CASE WHEN rec IN cityRecs THEN 2 ELSE 0 END AS cityScore,
               CASE WHEN rec IN tasteRecs THEN 2 ELSE 0 END AS tasteScore

          WITH rec, (mutualScore + cityScore + tasteScore) AS totalScore

          RETURN DISTINCT rec
          ORDER BY totalScore DESC, rec.followersCount DESC
          SKIP $skip
          LIMIT $limit
      """)
  List<UserProfile> findCombinedRecommendations(
      @Param("userId") String userId,
      @Param("skip") int skip,
      @Param("limit") int limit);

  // ============================================
  // 5.6 KIỂM TRA ĐÃ FOLLOW
  // ============================================

  /**
   * Kiểm tra xem user A đã follow user B chưa
   */
  @Query("""
          MATCH (a:user_profile {id: $fromId})-[r:follows]->(b:user_profile {id: $toId})
          RETURN COUNT(r) > 0
      """)
  boolean isFollowing(@Param("fromId") String fromId, @Param("toId") String toId);

  /**
   * Lấy danh sách IDs của những người mà user đang follow
   */
  @Query("""
          MATCH (me:user_profile {id: $userId})-[:follows]->(following:user_profile)
          RETURN following.id
      """)
  List<String> findFollowingIds(@Param("userId") String userId);

  // ============================================
  // CẬP NHẬT SỐ ĐẾM DENORMALIZED
  // ============================================

  /**
   * Cập nhật số followers và following count
   */
  @Query("""
          MATCH (u:user_profile {id: $userId})
          OPTIONAL MATCH (follower:user_profile)-[:follows]->(u)
          OPTIONAL MATCH (u)-[:follows]->(following:user_profile)
          WITH u, COUNT(DISTINCT follower) AS followers, COUNT(DISTINCT following) AS following
          SET u.followersCount = followers, u.followingCount = following
          RETURN u
      """)
  UserProfile updateFollowCounts(@Param("userId") String userId);

  /**
   * Cập nhật tất cả follow counts (cho migration/batch job)
   */
  @Query("""
          MATCH (u:user_profile)
          OPTIONAL MATCH (follower:user_profile)-[:follows]->(u)
          OPTIONAL MATCH (u)-[:follows]->(following:user_profile)
          WITH u, COUNT(DISTINCT follower) AS followers, COUNT(DISTINCT following) AS following
          SET u.followersCount = followers, u.followingCount = following
          RETURN COUNT(u)
      """)
  long updateAllFollowCounts();

  /**
   * Lấy chỉ danh sách IDs của tất cả profiles (tránh load full entity)
   */
  @Query("MATCH (u:user_profile) RETURN u.id")
  List<String> findAllProfileIds();

  /**
   * Set city cho 1 user (dùng Cypher, không cần load entity)
   */
  @Query("MATCH (u:user_profile {id: $profileId}) SET u.city = $city")
  void setCity(@Param("profileId") String profileId, @Param("city") String city);

  /**
   * Đếm tổng số profiles
   */
  @Query("MATCH (u:user_profile) RETURN COUNT(u)")
  long countProfiles();

  interface MutualRecommendationProjection {
    String getId();

    String getUserId();

    String getUsername();

    String getFirstName();

    String getLastName();

    String getImageUrl();

    String getBio();

    String getCity();

    Integer getFollowersCount();

    Integer getFollowingCount();

    Integer getMutualConnections();

    List<String> getMutualNames();
  }

}
