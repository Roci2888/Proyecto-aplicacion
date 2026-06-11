package com.example.blogproject.repositories;

import com.example.blogproject.models.Blog;
import org.springframework.data.mongodb.repository.MongoRepository;

// Busca blog por userId (cada usuario tiene un blog)
public interface BlogRepository extends MongoRepository<Blog, String> {

    Blog findByUserId(String userId);

}