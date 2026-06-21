package com.blocnotas.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de Spring Security.
 *
 * - API REST stateless (sin sesiones HTTP, sin CSRF).
 * - CORS configurado para permitir el frontend en desarrollo.
 * - Rutas públicas: /api/auth/** (login y registro).
 * - Todas las demás rutas requieren Bearer JWT válido.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthEntryPoint authEntryPoint;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtils jwtUtils;

    public SecurityConfig(JwtAuthEntryPoint authEntryPoint,
            UserDetailsServiceImpl userDetailsService,
            JwtUtils jwtUtils) {
        this.authEntryPoint = authEntryPoint;
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
    }

    // ── Beans ─────────────────────────────────────────────────────────────────

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configuración CORS para desarrollo local y producción (Vercel).
     *
     * IMPORTANTE: Cuando allowCredentials=true, la spec CORS prohíbe usar
     * el wildcard "*" en allowedHeaders. Se debe usar una lista explícita,
     * de lo contrario el preflight OPTIONS falla silenciosamente en algunos
     * navegadores y el backend rechaza la petición con 403/401.
     *
     * exposedHeaders permite que el interceptor de Axios pueda leer headers
     * de la respuesta (ej. un futuro Refresh-Token header).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://bloc-notas-tau.vercel.app"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // Lista explícita requerida por la spec CORS cuando allowCredentials=true
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "X-Forwarded-For"
        ));
        // Exponer Authorization para que el cliente pueda leerlo si fuera necesario
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        // Cachear el preflight por 1 hora (evita OPTIONS extras)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ── Security Filter Chain ──────────────────────────────────────────────────

    /**
     * Cadena de filtros de seguridad.
     *
     * Causa raíz del 401 en local: Si el servidor se reinicia con una JWT_SECRET
     * diferente a la que generó el token (ej. cambio de variables de entorno),
     * JwtUtils.validateToken() falla, el SecurityContext queda vacío y Spring
     * devuelve 401 automáticamente aunque anyRequest().authenticated() esté bien.
     *
     * Solución: usar siempre la misma JWT_SECRET durante una sesión local, o
     * hacer logout → login cada vez que se reinicia el backend.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Deshabilitar CSRF (innecesario en APIs REST con JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Configurar CORS con la configuración explícita definida arriba
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Política sin estado (stateless): no se crean sesiones HTTP
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Manejador de errores 401: devuelve JSON en lugar de redirect HTML
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(authEntryPoint))

                // ── Reglas de autorización ──────────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas: login y registro (sin JWT)
                        .requestMatchers("/api/auth/**").permitAll()
                        // Rutas de notas: requieren JWT válido explícitamente
                        .requestMatchers("/api/notas/**").authenticated()
                        // Cualquier otra ruta futura también requiere autenticación
                        .anyRequest().authenticated()
                )

                // Proveedor de autenticación (BCrypt + UserDetailsService)
                .authenticationProvider(authenticationProvider())

                // JwtAuthFilter se ejecuta ANTES del filtro estándar de Spring Security.
                // Si el token es válido, setea el Authentication en el SecurityContext.
                // Si no hay token o es inválido, continúa sin autenticación → Spring
                // evalúa las reglas de autorización y devuelve 401.
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
