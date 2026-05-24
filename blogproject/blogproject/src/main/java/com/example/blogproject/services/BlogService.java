package com.example.blogproject.services;

import com.example.blogproject.models.Blog;
import com.example.blogproject.repositories.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlogService {
    @Autowired
    private BlogRepository blogRepository;

    public Blog save(Blog blog) {
        return blogRepository.save(blog);
    }

    public Blog findById(String id) {
        return blogRepository.findById(id).orElse(null);
    }

    public List<Blog> findAllByUserId(String userId) {
        return blogRepository.findAllByUserId(userId);
    }

    public List<Blog> findDraftsByUserId(String userId) {
        return blogRepository.findAllByUserIdAndStatus(userId, "DRAFT");
    }

    public Blog findPublicByUserId(String userId) {
        return blogRepository.findByUserIdAndStatus(userId, "PUBLIC");
    }

    //  Publicar una ficha
    // Regla: solo puede haber UNA ficha pública por usuario.
    // Si ya existe una publicada, la pasa a DRAFT antes de publicar la nueva.

    public Blog publish(String blogId, String userId) {

        Blog currentlyPublic = findPublicByUserId(userId);
        if (currentlyPublic != null && !currentlyPublic.getId().equals(blogId)) {
            currentlyPublic.setStatus("DRAFT");
            blogRepository.save(currentlyPublic);
        }

        Blog blog = findById(blogId);
        if (blog == null || !blog.getUserId().equals(userId)) {
            return null;
        }

        blog.setStatus("PUBLIC");
        return blogRepository.save(blog);
    }

    public Blog unpublish(String blogId, String userId) {
        Blog blog = findById(blogId);
        if (blog == null || !blog.getUserId().equals(userId)) {
            return null;
        }
        blog.setStatus("DRAFT");
        return blogRepository.save(blog);
    }

    public Blog update(String blogId, String userId,
                       String name, String description, String coverImageUrl) {
        Blog blog = findById(blogId);
        if (blog == null || !blog.getUserId().equals(userId)) {
            return null;
        }
        blog.setName(name);
        blog.setDescription(description);
        if (coverImageUrl != null && !coverImageUrl.isBlank()) {
            blog.setCoverImageUrl(coverImageUrl);
        }
        return blogRepository.save(blog);
    }

    public void delete(String blogId, String userId) {
        Blog blog = findById(blogId);
        if (blog != null && blog.getUserId().equals(userId)) {
            blogRepository.deleteById(blogId);
        }
    }
}