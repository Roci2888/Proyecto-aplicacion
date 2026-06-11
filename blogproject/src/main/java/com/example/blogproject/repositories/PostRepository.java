package com.example.blogproject.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.blogproject.models.Post;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {
    // Busca posts de un blog ordenados por fecha (nuevos primero)
    // (sin paginación)
    List<Post> findByBlogIdOrderByCreatedAtDesc(String blogId);

    // Lo mismo pero con paginación
    // para panel (paginado)
    Page<Post> findByBlogIdOrderByCreatedAtDesc(String blogId, Pageable pageable);

}