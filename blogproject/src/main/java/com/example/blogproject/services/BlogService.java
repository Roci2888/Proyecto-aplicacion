package com.example.blogproject.services;

import com.example.blogproject.models.Blog;
import com.example.blogproject.repositories.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlogService {

    @Autowired
    private BlogRepository blogRepository;

    //Guarda blog
    public void save(Blog blog) {
        blogRepository.save(blog);
    }

    //Busca blog de un usuario
    public Blog findByUserId(String userId) {
        return blogRepository.findByUserId(userId);
    }

    //Busca blog por ID
    public Blog findById(String id) {
        return blogRepository.findById(id).orElse(null);
    }
}