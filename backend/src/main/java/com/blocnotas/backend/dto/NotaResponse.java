package com.blocnotas.backend.dto;

import com.blocnotas.backend.entity.Nota;

import java.time.LocalDateTime;

/**
 * DTO de salida para representar una nota en las respuestas JSON.
 *
 * No expone la entidad JPA directamente (patrón DTO) para:
 * - Evitar lazy-loading exceptions durante la serialización.
 * - No exponer la contraseña del usuario anidado.
 * - Controlar exactamente qué campos se envían al frontend.
 */
public record NotaResponse(
        Long id,
        String contenido,
        LocalDateTime fechaCreacion,
        String ipOrigen,
        String username
) {
    /**
     * Factory method que convierte una entidad Nota en un NotaResponse.
     * Centraliza la lógica de mapeo en un solo lugar.
     *
     * @param nota la entidad JPA persistida
     * @return el DTO listo para serializar
     */
    public static NotaResponse from(Nota nota) {
        return new NotaResponse(
                nota.getId(),
                nota.getContenido(),
                nota.getFechaCreacion(),
                nota.getIpOrigen(),
                nota.getUsuario().getUsername()
        );
    }
}
