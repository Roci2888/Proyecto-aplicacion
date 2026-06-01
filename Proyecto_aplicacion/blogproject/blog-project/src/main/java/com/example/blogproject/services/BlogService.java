package com.example.blogproject.services;

import com.example.blogproject.models.Blog;
import com.example.blogproject.models.Post;
import com.example.blogproject.repositories.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlogService {

    @Autowired
    private BlogRepository blogRepository;

    public void save(Post blog) {
        blogRepository.save(blog);
    }

    public Blog findByUserId(String userId) {
        return blogRepository.findByUserId(userId);
    }

    public Blog findById(String id) {
        return blogRepository.findById(id).orElse(null);
    }
}