package com.blocnotas.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que intercepta cada petición HTTP para:
 * 1. Extraer el Bearer token del header Authorization.
 * 2. Validarlo usando {@link JwtUtils}.
 * 3. Cargar el usuario y establecer la autenticación en el SecurityContext.
 *
 * Extiende {@link OncePerRequestFilter} para garantizar ejecución una sola vez por petición.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        try {
            String jwt = parseJwt(request);

            if (jwt == null) {
                // Solo loggeamos rutas protegidas para no saturar la consola con /api/auth/**
                if (!path.startsWith("/api/auth/")) {
                    logger.debug("[JWT] Sin token en la petición: {} {}", request.getMethod(), path);
                }
                filterChain.doFilter(request, response);
                return;
            }

            logger.debug("[JWT] Token recibido para: {} {}", request.getMethod(), path);

            if (!jwtUtils.validateToken(jwt)) {
                // validateToken() ya logea el motivo exacto (expirado, malformado, etc.)
                logger.warn("[JWT] Token INVÁLIDO para: {} {}", request.getMethod(), path);
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtUtils.getUsernameFromToken(jwt);
            logger.debug("[JWT] Token válido para usuario '{}' en: {} {}", username, request.getMethod(), path);

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("[JWT] Autenticación establecida para '{}' ✓", username);

        } catch (Exception e) {
            logger.error("[JWT] Error inesperado procesando token en '{}': {}", path, e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token JWT del header Authorization en formato "Bearer <token>".
     *
     * @param request la petición HTTP entrante
     * @return el token crudo, o null si no está presente
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
