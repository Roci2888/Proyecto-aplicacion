package com.example.blogproject.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.blogproject.models.Post;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {

   // (sin paginación)
    List<Post> findByBlogIdOrderByCreatedAtDesc(String blogId);

    // para panel (paginado)
    Page<Post> findByBlogIdOrderByCreatedAtDesc(String blogId, Pageable pageable);

    void deleteByBlogId(String blogId);

}