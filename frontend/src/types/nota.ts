// ============================================================
// types/nota.ts
// Tipos TypeScript que reflejan exactamente los DTOs del backend.
// ============================================================

/** Representa una nota devuelta por el backend */
export interface Nota {
  id: number;
  contenido: string;
  /**
   * ISO 8601 con indicador UTC: "2026-06-25T16:25:00Z"
   * El sufijo "Z" garantiza que new Date(fechaCreacion) interprete correctamente
   * como UTC y convierta a la hora local del usuario sin desfases.
   */
  fechaCreacion: string;
  ipOrigen: string;
  username: string;
}

/** Body para crear o editar una nota */
export interface NotaRequest {
  contenido: string;
}

/** Entrada del heatmap: un día con su conteo de notas */
export interface EntradaActividad {
  /** Formato "YYYY-MM-DD" */
  fecha: string;
  count: number;
}

/** Respuesta del endpoint /api/notas/actividad */
export interface ActividadResponse {
  actividad: EntradaActividad[];
}

/**
 * Formato requerido por react-activity-calendar.
 * La librería espera `date` (string YYYY-MM-DD) y `count` (number).
 * `level` es 0-4 según la intensidad; lo calculamos en el frontend.
 */
export interface CalendarActivity {
  date: string;
  count: number;
  level: 0 | 1 | 2 | 3 | 4;
}
