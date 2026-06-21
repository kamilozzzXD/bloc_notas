package com.blocnotas.backend.security;

import com.blocnotas.backend.entity.Usuario;
import com.blocnotas.backend.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Implementación de {@link UserDetailsService} que carga los datos de usuario
 * desde la base de datos MySQL a través de {@link UsuarioRepository}.
 *
 * Spring Security usa este servicio durante el proceso de autenticación.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Carga un usuario por su username para Spring Security.
     *
     * @param username el nombre de usuario a buscar
     * @return UserDetails listo para que Spring Security lo use
     * @throws UsernameNotFoundException si no existe el usuario
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado: " + username));

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(Collections.emptyList()) // Sin roles por ahora
                .build();
    }
}
