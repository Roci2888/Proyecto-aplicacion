package com.example.blogproject.repositories;

import com.example.blogproject.models.Blog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BlogRepository extends MongoRepository<Blog, String> {

    List<Blog> findAllByUserId(String userId);

    List<Blog> findAllByUserIdAndStatus(String userId, String status);

    Blog findByUserIdAndStatus(String userId, String status);
}