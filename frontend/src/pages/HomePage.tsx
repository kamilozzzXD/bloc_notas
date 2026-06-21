import React from 'react';
import { useNavigate } from 'react-router-dom';
import NotasDashboard from '../components/NotasDashboard';
import styles from './HomePage.module.css';

/**
 * Página principal protegida.
 * Contiene el header con logout y el dashboard completo de notas.
 */
const HomePage: React.FC = () => {
  const navigate = useNavigate();
  const username = sessionStorage.getItem('username') ?? 'Usuario';

  const handleLogout = () => {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('username');
    navigate('/login');
  };

  return (
    <div className={styles.homeWrapper}>
      {/* ── Header fijo ──────────────────────────────── */}
      <header className={styles.header}>
        <div className={styles.headerLeft}>
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
          </svg>
          <span className={styles.brand}>Bloc de Notas</span>
        </div>

        <div className={styles.headerRight}>
          <div className={styles.userBadge}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <span className={styles.welcomeMsg}>{username}</span>
          </div>
          <button id="logout-btn" className={styles.logoutBtn} onClick={handleLogout}>
            Cerrar sesión
          </button>
        </div>
      </header>

      {/* ── Contenido principal ───────────────────────── */}
      <main className={styles.main}>
        <NotasDashboard />
      </main>
    </div>
  );
};

export default HomePage;
