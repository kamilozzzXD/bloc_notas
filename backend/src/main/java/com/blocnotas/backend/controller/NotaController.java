package com.blocnotas.backend.controller;

import com.blocnotas.backend.dto.ActividadResponse;
import com.blocnotas.backend.dto.MensajeResponse;
import com.blocnotas.backend.dto.NotaRequest;
import com.blocnotas.backend.dto.NotaResponse;
import com.blocnotas.backend.entity.Nota;
import com.blocnotas.backend.entity.Usuario;
import com.blocnotas.backend.repository.NotaRepository;
import com.blocnotas.backend.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para el CRUD de Notas.
 *
 * Todos los endpoints requieren JWT válido (configurado en SecurityConfig).
 * El usuario se extrae del SecurityContext vía el objeto Authentication.
 *
 * Endpoints:
 *   GET    /api/notas              → Todas las notas del usuario
 *   GET    /api/notas/dia?fecha=   → Notas de un día específico
 *   GET    /api/notas/actividad    → Datos de actividad para el heatmap
 *   POST   /api/notas              → Crear nueva nota (captura IP)
 *   PUT    /api/notas/{id}         → Editar nota (solo si es del mismo día en Bogotá)
 *   DELETE /api/notas/{id}         → Eliminar nota
 */
@RestController
@RequestMapping("/api/notas")
public class NotaController {

    /** Zona horaria de Colombia (UTC-5). Usada para validar edición por día. */
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private final NotaRepository notaRepository;
    private final UsuarioRepository usuarioRepository;

    public NotaController(NotaRepository notaRepository, UsuarioRepository usuarioRepository) {
        this.notaRepository = notaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ── Utilidades privadas ────────────────────────────────────────────────────

    /**
     * Obtiene la entidad Usuario autenticado desde el SecurityContext.
     * Lanza RuntimeException si el usuario no existe (no debería ocurrir nunca
     * si el token es válido, pero es defensa contra inconsistencias de DB).
     */
    private Usuario getUsuarioAutenticado(Authentication auth) {
        return usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en DB: " + auth.getName()));
    }

    /**
     * Extrae la IP real del cliente.
     *
     * Estrategia de prioridad:
     *  1. Header X-Forwarded-For (proxy inverso de Render/Koyeb)
     *  2. Header X-Real-IP (algunos proxies alternativos)
     *  3. request.getRemoteAddr() como último recurso
     *
     * El header X-Forwarded-For puede contener múltiples IPs separadas por coma
     * (ej. "cliente, proxy1, proxy2"). Solo tomamos la primera.
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Determina si una nota todavía puede editarse.
     *
     * Regla: La nota es editable si el día de su creación (convertido a
     * zona horaria America/Bogota) coincide con el día actual en Bogotá.
     *
     * @param fechaCreacion LocalDateTime de creación de la nota (en UTC del servidor)
     * @return true si la nota fue creada hoy en Bogotá
     */
    private boolean esEditableHoy(LocalDateTime fechaCreacion) {
        // Convertir la fecha de creación (sin zona) a ZonedDateTime en Bogotá
        ZonedDateTime creacionEnBogota = fechaCreacion.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZONA_BOGOTA);

        // Obtener el día de hoy en Bogotá
        LocalDate hoyEnBogota = ZonedDateTime.now(ZONA_BOGOTA).toLocalDate();

        return creacionEnBogota.toLocalDate().isEqual(hoyEnBogota);
    }

    // ── GET /api/notas ────────────────────────────────────────────────────────

    /**
     * Devuelve todas las notas del usuario autenticado, ordenadas de más reciente a más antigua.
     */
    @GetMapping
    public ResponseEntity<List<NotaResponse>> listarNotas(Authentication auth) {
        Usuario usuario = getUsuarioAutenticado(auth);

        List<NotaResponse> notas = notaRepository
                .findByUsuarioOrderByFechaCreacionDesc(usuario)
                .stream()
                .map(NotaResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(notas);
    }

    // ── GET /api/notas/dia?fecha=YYYY-MM-DD ────────────────────────────────────

    /**
     * Devuelve las notas del usuario creadas en un día específico.
     *
     * El parámetro "fecha" debe venir en formato ISO: YYYY-MM-DD.
     * El backend calcula el rango inicio/fin del día en UTC del servidor.
     *
     * @param fechaStr fecha en formato "YYYY-MM-DD"
     */
    @GetMapping("/dia")
    public ResponseEntity<?> notasPorDia(
            @RequestParam("fecha") String fechaStr,
            Authentication auth) {
        try {
            LocalDate fecha = LocalDate.parse(fechaStr);
            Usuario usuario = getUsuarioAutenticado(auth);

            LocalDateTime inicio = fecha.atStartOfDay();
            LocalDateTime fin = fecha.atTime(LocalTime.MAX);

            List<NotaResponse> notas = notaRepository
                    .findByUsuarioAndFechaCreacionBetweenOrderByFechaCreacionDesc(usuario, inicio, fin)
                    .stream()
                    .map(NotaResponse::from)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(notas);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MensajeResponse("Formato de fecha inválido. Use YYYY-MM-DD."));
        }
    }

    // ── GET /api/notas/actividad ──────────────────────────────────────────────

    /**
     * Devuelve el conteo de notas agrupado por día para el heatmap.
     * El frontend usa estos datos para pintar react-activity-calendar.
     */
    @GetMapping("/actividad")
    public ResponseEntity<ActividadResponse> actividad(Authentication auth) {
        Usuario usuario = getUsuarioAutenticado(auth);

        List<ActividadResponse.EntradaActividad> entradas = notaRepository
                .countNotasByDiaParaUsuario(usuario)
                .stream()
                .map(row -> new ActividadResponse.EntradaActividad(
                        (String) row[0],           // fecha "YYYY-MM-DD"
                        ((Number) row[1]).longValue() // count
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ActividadResponse(entradas));
    }

    // ── POST /api/notas ───────────────────────────────────────────────────────

    /**
     * Crea una nueva nota para el usuario autenticado.
     *
     * Captura la IP real usando X-Forwarded-For (proxy de Render) como prioridad.
     *
     * @param request  cuerpo JSON { "contenido": "..." }
     * @param servletRequest petición HTTP original (para extraer la IP)
     * @return 201 Created con la nota creada
     */
    @PostMapping
    public ResponseEntity<NotaResponse> crearNota(
            @Valid @RequestBody NotaRequest request,
            HttpServletRequest servletRequest,
            Authentication auth) {

        Usuario usuario = getUsuarioAutenticado(auth);
        String ip = extractClientIp(servletRequest);

        Nota nota = new Nota(request.contenido(), ip, usuario);
        Nota guardada = notaRepository.save(nota);

        return ResponseEntity.status(HttpStatus.CREATED).body(NotaResponse.from(guardada));
    }

    // ── PUT /api/notas/{id} ───────────────────────────────────────────────────

    /**
     * Actualiza el contenido de una nota existente.
     *
     * Validaciones:
     * 1. La nota debe existir y pertenecer al usuario autenticado (403 si no).
     * 2. La nota solo puede editarse si fue creada el mismo día en zona horaria
     *    America/Bogota (403 si es de un día anterior).
     *
     * @param id      ID de la nota a editar
     * @param request cuerpo JSON { "contenido": "..." }
     * @return 200 OK con la nota actualizada
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> editarNota(
            @PathVariable Long id,
            @Valid @RequestBody NotaRequest request,
            Authentication auth) {

        Nota nota = notaRepository.findById(id).orElse(null);

        // 404 si no existe
        if (nota == null) {
            return ResponseEntity.notFound().build();
        }

        // 403 si la nota pertenece a otro usuario
        if (!nota.getUsuario().getUsername().equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MensajeResponse("No tienes permiso para editar esta nota."));
        }

        // 403 si la nota es de un día anterior en zona horaria Bogotá
        if (!esEditableHoy(nota.getFechaCreacion())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MensajeResponse(
                            "Esta nota no puede editarse porque fue creada en un día anterior (hora Colombia)."));
        }

        nota.setContenido(request.contenido());
        Nota actualizada = notaRepository.save(nota);

        return ResponseEntity.ok(NotaResponse.from(actualizada));
    }

    // ── DELETE /api/notas/{id} ────────────────────────────────────────────────

    /**
     * Elimina una nota del usuario autenticado.
     *
     * @param id ID de la nota a eliminar
     * @return 204 No Content si se eliminó, 403/404 si hay problemas de permisos
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarNota(@PathVariable Long id, Authentication auth) {
        Nota nota = notaRepository.findById(id).orElse(null);

        if (nota == null) {
            return ResponseEntity.notFound().build();
        }

        if (!nota.getUsuario().getUsername().equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MensajeResponse("No tienes permiso para eliminar esta nota."));
        }

        notaRepository.delete(nota);
        return ResponseEntity.noContent().build();
    }
}
