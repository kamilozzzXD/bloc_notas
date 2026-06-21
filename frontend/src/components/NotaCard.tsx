import React, { useState } from 'react';
import { editarNota, eliminarNota } from '../api/notaApi';
import type { Nota } from '../types/nota';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';
import styles from './NotaCard.module.css';

interface Props {
  nota: Nota;
  /** Si es true, muestra botones de editar y eliminar */
  editable: boolean;
  /** Callback tras modificar o eliminar la nota */
  onCambio: () => void;
}

/**
 * Tarjeta que representa una nota individual.
 *
 * Modos:
 * - editable=true  → Muestra botones "Editar" y "Eliminar". Al editar, se convierte en un textarea inline.
 * - editable=false → Solo lectura, sin controles de edición.
 */
const NotaCard: React.FC<Props> = ({ nota, editable, onCambio }) => {
  const [modoEdicion, setModoEdicion] = useState(false);
  const [contenidoEdit, setContenidoEdit] = useState(nota.contenido);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const fechaFormateada = format(new Date(nota.fechaCreacion), "d 'de' MMMM yyyy 'a las' HH:mm", { locale: es });

  // ── Guardar edición ────────────────────────────────────────────
  const handleGuardar = async () => {
    if (!contenidoEdit.trim()) return;
    setLoading(true);
    setError('');
    try {
      await editarNota(nota.id, { contenido: contenidoEdit.trim() });
      setModoEdicion(false);
      onCambio();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { mensaje?: string }; status?: number } };
      if (axiosErr.response?.status === 403) {
        setError(axiosErr.response.data?.mensaje ?? 'No puedes editar esta nota.');
      } else {
        setError('Error al guardar. Intenta de nuevo.');
      }
    } finally {
      setLoading(false);
    }
  };

  // ── Cancelar edición ───────────────────────────────────────────
  const handleCancelar = () => {
    setContenidoEdit(nota.contenido);
    setError('');
    setModoEdicion(false);
  };

  // ── Eliminar ───────────────────────────────────────────────────
  const handleEliminar = async () => {
    if (!window.confirm('¿Eliminar esta nota?')) return;
    setLoading(true);
    try {
      await eliminarNota(nota.id);
      onCambio();
    } catch {
      setError('Error al eliminar.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      id={`nota-card-${nota.id}`}
      className={`${styles.card} ${editable ? styles.cardEditable : styles.cardReadOnly}`}
    >
      {/* Contenido */}
      {modoEdicion ? (
        <textarea
          id={`nota-edit-textarea-${nota.id}`}
          className={styles.editTextarea}
          value={contenidoEdit}
          onChange={(e) => setContenidoEdit(e.target.value)}
          maxLength={5000}
          rows={4}
          autoFocus
        />
      ) : (
        <p className={styles.contenido}>{nota.contenido}</p>
      )}

      {/* Error inline */}
      {error && <p className={styles.error} role="alert">{error}</p>}

      {/* Pie: fecha + acciones */}
      <div className={styles.footer}>
        <span className={styles.fecha}>{fechaFormateada}</span>

        {editable && (
          <div className={styles.acciones}>
            {modoEdicion ? (
              <>
                <button
                  id={`nota-guardar-${nota.id}`}
                  className={`${styles.btn} ${styles.btnGuardar}`}
                  onClick={handleGuardar}
                  disabled={loading}
                >
                  {loading ? <span className={styles.spinner} /> : 'Guardar'}
                </button>
                <button
                  id={`nota-cancelar-${nota.id}`}
                  className={`${styles.btn} ${styles.btnCancelar}`}
                  onClick={handleCancelar}
                  disabled={loading}
                >
                  Cancelar
                </button>
              </>
            ) : (
              <>
                <button
                  id={`nota-editar-${nota.id}`}
                  className={`${styles.btn} ${styles.btnEditar}`}
                  onClick={() => setModoEdicion(true)}
                  disabled={loading}
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                  </svg>
                  Editar
                </button>
                <button
                  id={`nota-eliminar-${nota.id}`}
                  className={`${styles.btn} ${styles.btnEliminar}`}
                  onClick={handleEliminar}
                  disabled={loading}
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polyline points="3 6 5 6 21 6" />
                    <path d="M19 6l-1 14H6L5 6" />
                    <path d="M10 11v6M14 11v6" />
                    <path d="M9 6V4h6v2" />
                  </svg>
                  Eliminar
                </button>
              </>
            )}
          </div>
        )}
      </div>

      {/* Indicador de solo lectura */}
      {!editable && (
        <span className={styles.soloLecturaBadge}>Solo lectura</span>
      )}
    </div>
  );
};

export default NotaCard;
