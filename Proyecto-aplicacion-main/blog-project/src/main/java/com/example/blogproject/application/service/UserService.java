package com.example.blogproject.application.service;

import com.example.blogproject.domain.model.User;
import com.example.blogproject.infrastructure.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
            if (username == null || password == null || username.trim().isEmpty()) {
                return null;
            }

            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (passwordMatches(password, user.getPassword())) {
                    if (!isBCryptHash(user.getPassword())) {
                        user.setPassword(passwordEncoder.encode(password));
                        userRepository.save(user);
                    }
                    return user;
                }
            }

            return null;
        } catch (Exception e) {
            // Relanzar la excepción para que el controlador la maneje
            throw new RuntimeException("Error al intentar iniciar sesión: " + e.getMessage(), e);
        }
    }
    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (isBCryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        // Contraseña heredada en texto plano.
        return storedPassword.equals(rawPassword);
    }

    private boolean isBCryptHash(String value) {
        return value != null
                && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
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