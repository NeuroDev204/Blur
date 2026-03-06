package com.contentservice.post.repository;

import com.contentservice.post.entity.PostSave;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostSaveRepository extends MongoRepository<PostSave, String> {
    PostSave findByPostId(String postId);
}
