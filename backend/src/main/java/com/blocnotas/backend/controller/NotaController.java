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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

// Controlador REST para el CRUD de Notas. Todos los endpoints requieren JWT válido.
@RestController
@RequestMapping("/api/notas")
public class NotaController {

    // Zona horaria Colombia (UTC-5)
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FMT_BOGOTA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NotaRepository notaRepository;
    private final UsuarioRepository usuarioRepository;

    public NotaController(NotaRepository notaRepository, UsuarioRepository usuarioRepository) {
        this.notaRepository = notaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ── Utilidades privadas ──────────────────────────────────────────────────

    // Obtiene el usuario autenticado desde el SecurityContext
    private Usuario getUsuarioAutenticado(Authentication auth) {
        return usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en DB: " + auth.getName()));
    }

    // Extrae la IP real del cliente: X-Forwarded-For → X-Real-IP → remoteAddr
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

    // Una nota es editable si fue creada el mismo día en zona horaria Bogotá
    private boolean esEditableHoy(LocalDateTime fechaCreacion) {
        // fechaCreacion está almacenada en UTC explícito (ZoneOffset.UTC en Nota.java)
        ZonedDateTime creacionEnBogota = fechaCreacion.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZONA_BOGOTA);
        LocalDate hoyEnBogota = ZonedDateTime.now(ZONA_BOGOTA).toLocalDate();
        return creacionEnBogota.toLocalDate().isEqual(hoyEnBogota);
    }

    // ── GET /api/notas ───────────────────────────────────────────────────────

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

    // ── GET /api/notas/dia?fecha=YYYY-MM-DD ─────────────────────────────────

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

    // ── GET /api/notas/actividad ─────────────────────────────────────────────

    @GetMapping("/actividad")
    public ResponseEntity<ActividadResponse> actividad(Authentication auth) {
        Usuario usuario = getUsuarioAutenticado(auth);
        List<ActividadResponse.EntradaActividad> entradas = notaRepository
                .countNotasByDiaParaUsuario(usuario)
                .stream()
                .map(row -> new ActividadResponse.EntradaActividad(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ActividadResponse(entradas));
    }

    // ── GET /api/notas/exportar ──────────────────────────────────────────────

    // Genera y descarga un archivo .txt con todas las notas del usuario.
    // Cada nota incluye fecha/hora en zona Bogotá, IP de origen y contenido.
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportarDiario(Authentication auth) {
        Usuario usuario = getUsuarioAutenticado(auth);

        List<Nota> notas = notaRepository.findByUsuarioOrderByFechaCreacionDesc(usuario);

        StringBuilder sb = new StringBuilder();
        sb.append("=== MIS NOTAS - ").append(usuario.getUsername()).append(" ===\n");
        sb.append("Exportado el: ").append(ZonedDateTime.now(ZONA_BOGOTA).format(FMT_BOGOTA))
          .append(" (Hora Colombia)\n");
        sb.append("Total de notas: ").append(notas.size()).append("\n");
        sb.append("=".repeat(45)).append("\n\n");

        for (Nota nota : notas) {
            // fechaCreacion está en UTC explícito → convertir a Bogotá para la exportación
            ZonedDateTime fechaBogota = nota.getFechaCreacion()
                    .atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(ZONA_BOGOTA);

            sb.append("Fecha y Hora : ").append(fechaBogota.format(FMT_BOGOTA)).append(" (Bogotá)\n");
            sb.append("IP de origen : ").append(nota.getIpOrigen()).append("\n");
            sb.append("Contenido    :\n").append(nota.getContenido()).append("\n");
            sb.append("-".repeat(45)).append("\n\n");
        }

        byte[] contenido = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "notas_" + usuario.getUsername() + ".txt");
        headers.setContentLength(contenido.length);

        return ResponseEntity.ok().headers(headers).body(contenido);
    }

    // ── POST /api/notas ──────────────────────────────────────────────────────

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

    // ── PUT /api/notas/{id} ──────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<?> editarNota(
            @PathVariable Long id,
            @Valid @RequestBody NotaRequest request,
            Authentication auth) {

        Nota nota = notaRepository.findById(id).orElse(null);

        if (nota == null) return ResponseEntity.notFound().build();

        if (!nota.getUsuario().getUsername().equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MensajeResponse("No tienes permiso para editar esta nota."));
        }

        // Solo editable si fue creada el mismo día en hora Bogotá
        if (!esEditableHoy(nota.getFechaCreacion())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MensajeResponse(
                            "Esta nota no puede editarse porque fue creada en un día anterior (hora Colombia)."));
        }

        nota.setContenido(request.contenido());
        Nota actualizada = notaRepository.save(nota);
        return ResponseEntity.ok(NotaResponse.from(actualizada));
    }

    // ── DELETE /api/notas/{id} ───────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarNota(@PathVariable Long id, Authentication auth) {
        Nota nota = notaRepository.findById(id).orElse(null);

        if (nota == null) return ResponseEntity.notFound().build();

        if (!nota.getUsuario().getUsername().equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MensajeResponse("No tienes permiso para eliminar esta nota."));
        }

        notaRepository.delete(nota);
        return ResponseEntity.noContent().build();
    }
}
