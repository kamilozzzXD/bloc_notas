package com.blocnotas.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear o actualizar una nota.
 * Solo recibe el contenido; el resto de campos (ip, usuario, fecha)
 * se asignan en el controlador.
 */
public record NotaRequest(
        @NotBlank(message = "El contenido no puede estar vacío")
        @Size(max = 5000, message = "El contenido no puede superar 5000 caracteres")
        String contenido
) {}
