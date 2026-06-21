package com.blocnotas.backend.controller;

import com.blocnotas.backend.dto.AuthRequest;
import com.blocnotas.backend.dto.AuthResponse;
import com.blocnotas.backend.dto.MensajeResponse;
import com.blocnotas.backend.entity.Usuario;
import com.blocnotas.backend.repository.UsuarioRepository;
import com.blocnotas.backend.security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación.
 *
 * Endpoints públicos (sin JWT):
 *   POST /api/auth/register  → Registra un nuevo usuario
 *   POST /api/auth/login     → Autentica y devuelve un JWT
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager,
                          UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    // ── POST /api/auth/register ───────────────────────────────────────────────

    /**
     * Registra un nuevo usuario en la base de datos.
     * La contraseña se encripta con BCrypt antes de persistirla.
     *
     * @param request { username, password }
     * @return 201 Created con mensaje de éxito, o 409 Conflict si el username ya existe
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new MensajeResponse("El nombre de usuario '" + request.username() + "' ya está en uso."));
        }

        Usuario nuevoUsuario = new Usuario(
                request.username(),
                passwordEncoder.encode(request.password())
        );
        usuarioRepository.save(nuevoUsuario);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MensajeResponse("Usuario registrado correctamente. ¡Ahora puedes iniciar sesión!"));
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    /**
     * Autentica al usuario y devuelve un token JWT.
     *
     * Spring Security valida las credenciales automáticamente mediante
     * {@link AuthenticationManager}. Si falla, lanza una excepción que
     * Spring convierte en HTTP 401.
     *
     * @param request { username, password }
     * @return 200 OK con { token, tipo, username }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails.getUsername());

        return ResponseEntity.ok(new AuthResponse(token, userDetails.getUsername()));
    }
}
