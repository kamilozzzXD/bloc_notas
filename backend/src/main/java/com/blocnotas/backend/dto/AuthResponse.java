package com.blocnotas.backend.dto;

/**
 * DTO para la respuesta de un login exitoso.
 * Devuelve { "token": "...", "tipo": "Bearer", "username": "..." } al frontend.
 */
public record AuthResponse(
        String token,
        String tipo,
        String username
) {
    /**
     * Constructor de conveniencia que establece el tipo como "Bearer" por defecto.
     */
    public AuthResponse(String token, String username) {
        this(token, "Bearer", username);
    }
}
