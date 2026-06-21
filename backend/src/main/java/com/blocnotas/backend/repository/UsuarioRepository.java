package com.blocnotas.backend.repository;

import com.blocnotas.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Usuario.
 * Spring Data genera la implementación automáticamente.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su nombre de usuario.
     *
     * @param username el nombre de usuario a buscar
     * @return Optional con el usuario si existe
     */
    Optional<Usuario> findByUsername(String username);

    /**
     * Verifica si ya existe un usuario con el username dado.
     *
     * @param username el nombre de usuario a verificar
     * @return true si ya existe
     */
    boolean existsByUsername(String username);
}
