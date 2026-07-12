package com.example.blogproject.infrastructure.persistence;

import com.example.blogproject.domain.model.Blog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BlogRepository extends MongoRepository<Blog, String> {

    Blog findByUserId(String userId);

}