package com.example.blogproject.services;

import com.example.blogproject.models.Post;
import com.example.blogproject.repositories.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;


import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    // 🔹 guardar post
    public void save(Post post) {
        postRepository.save(post);
    }

    //Lista posts de un blog
    public List<Post> findByBlogId(String blogId) {
        return postRepository.findByBlogIdOrderByCreatedAtDesc(blogId);
    }

    // 🔹 eliminar post
    public void deleteById(String id) {
        postRepository.deleteById(id);
    }

    // 🔹 obtener un post
    public Post findById(String id) {
        return postRepository.findById(id).orElse(null);
    }

    // Posts paginados
    public Page<Post> getPostsByBlogPaged(String blogId, int page, int size) {
        return postRepository.findByBlogIdOrderByCreatedAtDesc(
                blogId,
                PageRequest.of(page, size)
        );
    }
}