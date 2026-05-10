package com.example.blogproject.repositories;

import com.example.blogproject.models.Blog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BlogRepository extends MongoRepository<Blog, String> {

    Blog findByUserId(String userId);

}