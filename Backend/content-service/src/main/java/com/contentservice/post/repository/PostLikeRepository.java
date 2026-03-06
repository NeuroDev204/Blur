package com.contentservice.post.repository;

import com.contentservice.post.entity.PostLike;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostLikeRepository extends MongoRepository<PostLike, String> {
    // ✅ Kiểm tra người dùng đã like bài viết chưa
    boolean existsByUserIdAndPostId(String userId, String postId);

    // ✅ Tìm bản ghi like của người dùng cho bài viết
    PostLike findByUserIdAndPostId(String userId, String postId);

    List<PostLike> findAllByPostId(String postId);
}
