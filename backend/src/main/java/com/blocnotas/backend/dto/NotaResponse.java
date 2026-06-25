package com.blocnotas.backend.dto;

import com.blocnotas.backend.entity.Nota;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * DTO de salida para representar una nota en las respuestas JSON.
 *
 * No expone la entidad JPA directamente (patrón DTO) para:
 * - Evitar lazy-loading exceptions durante la serialización.
 * - No exponer la contraseña del usuario anidado.
 * - Controlar exactamente qué campos se envían al frontend.
 *
 * IMPORTANTE — zona horaria en el JSON:
 * Se usa OffsetDateTime (con ZoneOffset.UTC) en lugar de LocalDateTime para que
 * Jackson serialice la fecha con el sufijo "Z" (e.g. "2026-06-25T16:25:00Z").
 * Eso le indica al frontend que la hora está en UTC, por lo que el navegador
 * la convierte automáticamente a la hora local del usuario sin desfases.
 */
public record NotaResponse(
        Long id,
        String contenido,
        OffsetDateTime fechaCreacion,
        String ipOrigen,
        String username
) {
    /**
     * Factory method que convierte una entidad Nota en un NotaResponse.
     * La fechaCreacion (LocalDateTime en UTC) se envuelve con ZoneOffset.UTC
     * para que Jackson la serialice con el indicador de zona "Z".
     *
     * @param nota la entidad JPA persistida
     * @return el DTO listo para serializar
     */
    public static NotaResponse from(Nota nota) {
        return new NotaResponse(
                nota.getId(),
                nota.getContenido(),
                nota.getFechaCreacion().atOffset(ZoneOffset.UTC),
                nota.getIpOrigen(),
                nota.getUsuario().getUsername()
        );
    }
}
