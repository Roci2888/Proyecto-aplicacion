package com.example.blogproject.services;

import com.example.blogproject.models.User;
import com.example.blogproject.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User save(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists: " + user.getUsername());
        }

        user.setRole("USER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        // Este metodo ahora es SEGURO porque solo hay 1 usuario por username
        try{
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (username == null || password == null || username.trim().isEmpty()) {
                return null;
            }

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (passwordEncoder.matches(password, user.getPassword())) {
                    return user;
                }
            }

            return null;
        } catch (Exception e) {
            // Relanzar la excepción para que el controlador la maneje
            throw new RuntimeException("Error al intentar iniciar sesión: " + e.getMessage(), e);
        }
    }

    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}
