package com.example.blogproject.application.service;

import com.example.blogproject.domain.model.Blog;
import com.example.blogproject.infrastructure.persistence.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlogService {

    @Autowired
    private BlogRepository blogRepository;

    public void save(Blog blog) {
        blogRepository.save(blog);
    }

    public Blog findByUserId(String userId) {
        return blogRepository.findByUserId(userId);
    }

    public Blog findById(String id) {
        return blogRepository.findById(id).orElse(null);
    }
}