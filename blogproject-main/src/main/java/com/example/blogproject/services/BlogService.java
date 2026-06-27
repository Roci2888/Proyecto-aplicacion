package com.example.blogproject.services;

import com.example.blogproject.models.Blog;
import com.example.blogproject.repositories.BlogRepository;
import com.example.blogproject.repositories.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlogService {

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private PostRepository postRepository;

    public void save(Blog blog) {
        blogRepository.save(blog);
    }

    public Blog findByUserId(String userId) {
        return blogRepository.findByUserId(userId);
    }

    public Blog findById(String id) {
        return blogRepository.findById(id).orElse(null);
    }

    public void deleteBlog(String id) {

        postRepository.deleteByBlogId(id);

        blogRepository.deleteById(id);
    }
}