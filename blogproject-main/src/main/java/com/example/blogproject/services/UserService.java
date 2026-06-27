package com.example.blogproject.services;

import com.example.blogproject.models.User;
import com.example.blogproject.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User save(User user) {
        user.setRole("USER"); // por defecto
        return userRepository.save(user);
    }

    public User login(String username, String password) {

    System.out.println("Username recibido: " + username);

    User user = userRepository.findByUsername(username).orElse(null);

    System.out.println("Usuario encontrado: " + (user != null));

    if (user != null) {
        System.out.println("Password BD: [" + user.getPassword() + "]");
        System.out.println("Password recibida: [" + password + "]");
    }

    if (user != null && user.getPassword().equals(password)) {
        return user;
    }

    return null;
}

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}
