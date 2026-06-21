import React, { useEffect, useState, useCallback } from 'react';
import { ActivityCalendar } from 'react-activity-calendar';
import Calendar from 'react-calendar';
import 'react-calendar/dist/Calendar.css';
import { obtenerActividad, obtenerNotasPorDia, exportarDiario } from '../api/notaApi';
import type { CalendarActivity } from '../types/nota';
import type { Nota } from '../types/nota';
import NotaForm from './NotaForm';
import NotaCard from './NotaCard';
import styles from './NotasDashboard.module.css';
import { format, parseISO } from 'date-fns';
import { es } from 'date-fns/locale';

// Convierte el conteo de notas en nivel 0-4 para el heatmap
function countToLevel(count: number): CalendarActivity['level'] {
  if (count === 0) return 0;
  if (count === 1) return 1;
  if (count <= 3) return 2;
  if (count <= 6) return 3;
  return 4;
}

// Dashboard principal: heatmap, calendario plegable, lista de notas y formulario
const NotasDashboard: React.FC = () => {
  const hoy = format(new Date(), 'yyyy-MM-dd');

  const [diaSeleccionado, setDiaSeleccionado] = useState<string>(hoy);
  const [calendarVisible, setCalendarVisible] = useState(false); // oculto por defecto
  const [exportando, setExportando] = useState(false);

  // Datos del heatmap
  const [actividadData, setActividadData] = useState<CalendarActivity[]>([]);
  const [cargandoActividad, setCargandoActividad] = useState(true);

  // Notas del día seleccionado
  const [notas, setNotas] = useState<Nota[]>([]);
  const [cargandoNotas, setCargandoNotas] = useState(false);

  // ── Carga de actividad del heatmap ─────────────────────────────────────
  const cargarActividad = useCallback(async () => {
    try {
      setCargandoActividad(true);
      const data = await obtenerActividad();

      const actividades: CalendarActivity[] = data.actividad.map((e) => ({
        date: e.fecha,
        count: Number(e.count),
        level: countToLevel(Number(e.count)),
      }));

      // react-activity-calendar requiere al menos el día de hoy en el array
      const tieneHoy = actividades.some((a) => a.date === hoy);
      if (!tieneHoy) {
        actividades.push({ date: hoy, count: 0, level: 0 });
      }

      setActividadData(actividades.sort((a, b) => a.date.localeCompare(b.date)));
    } catch {
      // Si falla, ponemos al menos hoy para evitar errores en la librería
      setActividadData([{ date: hoy, count: 0, level: 0 }]);
    } finally {
      setCargandoActividad(false);
    }
  }, [hoy]);

  // ── Carga de notas del día seleccionado ─────────────────────────────────
  const cargarNotasDia = useCallback(async (fecha: string) => {
    setCargandoNotas(true);
    try {
      const data = await obtenerNotasPorDia(fecha);
      setNotas(data);
    } catch {
      setNotas([]);
    } finally {
      setCargandoNotas(false);
    }
  }, []);

  // Efecto inicial
  useEffect(() => {
    cargarActividad();
  }, [cargarActividad]);

  useEffect(() => {
    cargarNotasDia(diaSeleccionado);
  }, [diaSeleccionado, cargarNotasDia]);

  /** Cuando el usuario hace clic en un día del calendario */
  const handleCalendarChange = (value: Date | Date[] | null) => {
    if (!value || Array.isArray(value)) return;
    setDiaSeleccionado(format(value, 'yyyy-MM-dd'));
  };

  /** Refresca notas y heatmap tras crear o editar una nota */
  const handleCambioNota = () => {
    cargarActividad();
    cargarNotasDia(diaSeleccionado);
  };

  // Descarga el diario como archivo .txt
  const handleExportar = async () => {
    setExportando(true);
    try {
      const blob = await exportarDiario();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'mis_notas.txt';
      link.click();
      URL.revokeObjectURL(url);
    } finally {
      setExportando(false);
    }
  };

  const esDiaHoy = diaSeleccionado === hoy;

  const tituloSeccion = esDiaHoy
    ? '📝 Hoy'
    : format(parseISO(diaSeleccionado), "d 'de' MMMM yyyy", { locale: es });

  return (
    <div className={styles.dashboard}>

      {/* Heatmap */}
      <section className={styles.heatmapSection}>
        <h2 className={styles.sectionTitle}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
            <line x1="16" y1="2" x2="16" y2="6" />
            <line x1="8" y1="2" x2="8" y2="6" />
            <line x1="3" y1="10" x2="21" y2="10" />
          </svg>
          Actividad de notas
        </h2>

        {cargandoActividad ? (
          <div className={styles.loadingPlaceholder}>Cargando heatmap...</div>
        ) : (
          <div className={styles.heatmapWrapper}>
            <ActivityCalendar
              data={actividadData}
              colorScheme="dark"
              theme={{
                dark: ['hsl(225, 40%, 10%)', 'hsl(217, 70%, 28%)', 'hsl(217, 80%, 40%)', 'hsl(217, 85%, 52%)', 'hsl(217, 91%, 65%)'],
              }}
              labels={{
                totalCount: '{{count}} notas en {{year}}',
                legend: { less: 'Menos', more: 'Más' },
              }}
              showWeekdayLabels
              fontSize={12}
            />
          </div>
        )}
      </section>

      {/* Layout: calendario + notas */}
      <div className={styles.mainLayout}>

        {/* Calendario mensual plegable */}
        <aside className={styles.calendarAside}>
          <button
            id="btn-toggle-calendario"
            className={styles.calendarToggle}
            onClick={() => setCalendarVisible(v => !v)}
            aria-expanded={calendarVisible}
          >
            📅 Consultar Día
            <span className={`${styles.toggleIcon} ${calendarVisible ? styles.toggleIconOpen : ''}`}>▾</span>
          </button>

          {calendarVisible && (
            <Calendar
              onChange={handleCalendarChange as (value: unknown) => void}
              value={parseISO(diaSeleccionado)}
              locale="es-CO"
              maxDate={new Date()}
              className={styles.reactCalendar}
              tileClassName={({ date }) => {
                const dateStr = format(date, 'yyyy-MM-dd');
                const tieneNotas = actividadData.some((a) => a.date === dateStr && a.count > 0);
                return tieneNotas ? styles.tileConNotas : null;
              }}
            />
          )}
        </aside>

        {/* Lista de notas del día */}
        <section className={styles.notasSection}>
          <div className={styles.notasSectionHeader}>
            <h2 className={styles.sectionTitle}>
              {tituloSeccion}
              {notas.length > 0 && (
                <span className={styles.badge}>{notas.length}</span>
              )}
            </h2>
            <button
              id="btn-exportar-diario"
              className={styles.exportBtn}
              onClick={handleExportar}
              disabled={exportando}
              title="Descargar todas tus notas como archivo de texto"
            >
              {exportando ? '⏳ Exportando...' : '📥 Exportar Notas'}
            </button>
          </div>

          {/* Formulario (solo si es hoy) */}
          {esDiaHoy && (
            <div className={styles.formWrapper}>
              <NotaForm onNotaCreada={handleCambioNota} />
            </div>
          )}

          {/* Estado de carga */}
          {cargandoNotas ? (
            <div className={styles.loadingNotas}>
              <span className={styles.spinner} />
              Cargando notas...
            </div>
          ) : notas.length === 0 ? (
            <div className={styles.sinNotas}>
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" opacity="0.35">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
              <p>
                {esDiaHoy
                  ? 'No hay notas hoy. ¡Escribe tu primera nota!'
                  : 'No hay notas para este día.'}
              </p>
            </div>
          ) : (
            <div className={styles.notasList}>
              {notas.map((nota) => (
                <NotaCard
                  key={nota.id}
                  nota={nota}
                  editable={esDiaHoy}
                  onCambio={handleCambioNota}
                />
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default NotasDashboard;
