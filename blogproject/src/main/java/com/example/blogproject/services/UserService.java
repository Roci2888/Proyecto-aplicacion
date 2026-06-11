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

    //Asigna rol "USER" y guarda
    public User save(User user) {
        user.setRole("USER"); // asigna rol
        return userRepository.save(user);
    }

    //Verifica credenciales para el inicio de sesion
    public User login(String username, String password) {

        // Busca al usuario en la base de datos o resulta nulo
        User user = userRepository.findByUsername(username).orElse(null);

        // Si el usuario existe (no es nulo) Y además la contraseña coincide
        if (user != null && user.getPassword().equals(password)) {
            return user; // ... la autenticación es exitosa, devuelve el objeto del usuario completo
        }

        // Si el usuario no existe o la contraseña es incorrecta devuelve null
        return null;
    }

    //Lista todos los usuarios (para admin)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    //Elimina usuario por ID
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}