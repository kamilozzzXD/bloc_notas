import React from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './HomePage.module.css';

/**
 * Página de inicio (protegida por JWT).
 * Muestra la bienvenida al usuario y un botón de cierre de sesión.
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
      <header className={styles.header}>
        <div className={styles.headerLeft}>
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
          </svg>
          <span className={styles.brand}>Bloc de Notas</span>
        </div>
        <div className={styles.headerRight}>
          <span className={styles.welcomeMsg}>Hola, <strong>{username}</strong></span>
          <button id="logout-btn" className={styles.logoutBtn} onClick={handleLogout}>
            Cerrar sesión
          </button>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.emptyState}>
          <svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" opacity="0.4">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
            <polyline points="10 9 9 9 8 9" />
          </svg>
          <h2 className={styles.emptyTitle}>¡Bienvenido, {username}!</h2>
          <p className={styles.emptySubtitle}>
            Tu bloc de notas está listo. Las notas se agregarán aquí próximamente.
          </p>
        </div>
      </main>
    </div>
  );
};

export default HomePage;
