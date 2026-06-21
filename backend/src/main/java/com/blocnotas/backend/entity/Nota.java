package com.blocnotas.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Entidad que representa una Nota del usuario.
 *
 * Campos:
 *  - id             → PK auto-generada
 *  - contenido      → texto de la nota (máx. 5000 chars)
 *  - fechaCreacion  → timestamp de creación (UTC, almacenado tal cual)
 *  - ipOrigen       → IP real del cliente al momento de crear la nota
 *  - usuario        → relación ManyToOne con la entidad Usuario
 */
@Entity
@Table(name = "notas", indexes = {
        // Índice para optimizar las búsquedas por usuario
        @Index(name = "idx_nota_usuario", columnList = "usuario_id")
})
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 5000, message = "El contenido no puede superar los 5000 caracteres")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    /**
     * Fecha y hora de creación en UTC.
     * La conversión a zona horaria America/Bogota se hace en la capa de negocio
     * (NotaController) al validar edición, no aquí.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * IP real del cliente.
     * Se captura desde el header X-Forwarded-For (detrás de proxy Render)
     * o desde request.getRemoteAddr() como fallback.
     */
    @Column(nullable = false, length = 45) // 45 chars soporta IPv6
    private String ipOrigen;

    /**
     * Relación ManyToOne: muchas notas pertenecen a un usuario.
     * LAZY loading para evitar consultas innecesarias.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // ── Lifecycle ──────────────────────────────────────────────────────

    /**
     * Se ejecuta automáticamente antes de persistir la entidad.
     * Asigna la fecha de creación si aún no fue seteada.
     */
    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
    }

    // ── Constructores ──────────────────────────────────────────────────

    public Nota() {}

    public Nota(String contenido, String ipOrigen, Usuario usuario) {
        this.contenido = contenido;
        this.ipOrigen = ipOrigen;
        this.usuario = usuario;
    }

    // ── Getters y Setters ──────────────────────────────────────────────

    public Long getId() { return id; }

    public String getContenido() { return contenido; }

    public void setContenido(String contenido) { this.contenido = contenido; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getIpOrigen() { return ipOrigen; }

    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }

    public Usuario getUsuario() { return usuario; }

    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
