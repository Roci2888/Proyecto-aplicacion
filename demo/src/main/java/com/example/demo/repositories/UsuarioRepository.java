package com.example.demo.repositories;

import com.example.demo.usuario.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);
}
