package com.example.blogproject.repositories;

import com.example.blogproject.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

// Busca usuario por username (para login)
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
}