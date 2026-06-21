package com.blocnotas.backend.dto;

import java.util.List;

/**
 * DTO para el endpoint de actividad del heatmap.
 *
 * Devuelve una lista de entradas { fecha, count } que el frontend
 * convierte al formato que espera react-activity-calendar.
 */
public record ActividadResponse(List<EntradaActividad> actividad) {

    /**
     * Una entrada de actividad para un día específico.
     *
     * @param fecha  en formato "YYYY-MM-DD"
     * @param count  número de notas creadas ese día
     */
    public record EntradaActividad(String fecha, long count) {}
}
