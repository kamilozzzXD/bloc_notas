import React, { useState } from 'react';
import { crearNota } from '../api/notaApi';
import styles from './NotaForm.module.css';

interface Props {
  /** Callback ejecutado tras crear una nota exitosamente */
  onNotaCreada: () => void;
}

/**
 * Formulario para crear una nueva nota.
 * Solo visible cuando el usuario ha seleccionado el día de hoy.
 */
const NotaForm: React.FC<Props> = ({ onNotaCreada }) => {
  const [contenido, setContenido] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const MAX_CHARS = 5000;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!contenido.trim()) return;

    setLoading(true);
    setError('');

    try {
      await crearNota({ contenido: contenido.trim() });
      setContenido('');
      onNotaCreada();
    } catch {
      setError('No se pudo guardar la nota. Intenta de nuevo.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form id="nota-form" onSubmit={handleSubmit} className={styles.form}>
      <div className={styles.textareaWrapper}>
        <textarea
          id="nota-contenido"
          className={styles.textarea}
          placeholder="Escribe tu nota aquí..."
          value={contenido}
          onChange={(e) => setContenido(e.target.value)}
          maxLength={MAX_CHARS}
          rows={4}
          disabled={loading}
        />
        <span className={`${styles.charCount} ${contenido.length > MAX_CHARS * 0.9 ? styles.charCountWarning : ''}`}>
          {contenido.length}/{MAX_CHARS}
        </span>
      </div>

      {error && (
        <p id="nota-form-error" className={styles.error} role="alert">
          {error}
        </p>
      )}

      <button
        id="nota-submit"
        type="submit"
        className={styles.btn}
        disabled={loading || !contenido.trim()}
      >
        {loading ? <span className={styles.spinner} /> : (
          <>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            Guardar nota
          </>
        )}
      </button>
    </form>
  );
};

export default NotaForm;
