import apiClient from './apiClient';
import type { ActividadResponse, Nota, NotaRequest } from '../types/nota';

/**
 * Obtiene todas las notas del usuario autenticado.
 * GET /api/notas
 */
export async function obtenerNotas(): Promise<Nota[]> {
  const res = await apiClient.get<Nota[]>('/api/notas');
  return res.data;
}

/**
 * Obtiene las notas del usuario para un día específico.
 * GET /api/notas/dia?fecha=YYYY-MM-DD
 *
 * @param fecha string en formato "YYYY-MM-DD"
 */
export async function obtenerNotasPorDia(fecha: string): Promise<Nota[]> {
  const res = await apiClient.get<Nota[]>('/api/notas/dia', { params: { fecha } });
  return res.data;
}

/**
 * Obtiene los datos de actividad para el heatmap.
 * GET /api/notas/actividad
 */
export async function obtenerActividad(): Promise<ActividadResponse> {
  const res = await apiClient.get<ActividadResponse>('/api/notas/actividad');
  return res.data;
}

/**
 * Crea una nueva nota.
 * POST /api/notas
 *
 * @param data { contenido: string }
 */
export async function crearNota(data: NotaRequest): Promise<Nota> {
  const res = await apiClient.post<Nota>('/api/notas', data);
  return res.data;
}

/**
 * Edita el contenido de una nota existente.
 * PUT /api/notas/{id}
 *
 * @param id      ID de la nota a editar
 * @param data    { contenido: string }
 */
export async function editarNota(id: number, data: NotaRequest): Promise<Nota> {
  const res = await apiClient.put<Nota>(`/api/notas/${id}`, data);
  return res.data;
}

// Elimina una nota por ID. DELETE /api/notas/{id}
export async function eliminarNota(id: number): Promise<void> {
  await apiClient.delete(`/api/notas/${id}`);
}

// Descarga todas las notas del usuario como archivo .txt. GET /api/notas/exportar
export async function exportarDiario(): Promise<Blob> {
  const res = await apiClient.get('/api/notas/exportar', { responseType: 'blob' });
  return res.data;
}
