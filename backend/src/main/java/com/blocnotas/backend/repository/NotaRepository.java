package com.blocnotas.backend.repository;

import com.blocnotas.backend.entity.Nota;
import com.blocnotas.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JPA para la entidad Nota.
 *
 * Spring Data genera la implementación SQL automáticamente a partir
 * de los nombres de los métodos y las anotaciones @Query.
 */
@Repository
public interface NotaRepository extends JpaRepository<Nota, Long> {

    /**
     * Devuelve todas las notas de un usuario, ordenadas de más reciente a más antigua.
     *
     * @param usuario el usuario propietario
     * @return lista de notas del usuario
     */
    List<Nota> findByUsuarioOrderByFechaCreacionDesc(Usuario usuario);

    /**
     * Devuelve las notas de un usuario creadas dentro de un rango de fechas.
     * Usado para filtrar las notas de un día específico.
     *
     * @param usuario el usuario propietario
     * @param inicio  inicio del rango (ej. 2024-06-21T00:00:00)
     * @param fin     fin del rango   (ej. 2024-06-21T23:59:59)
     * @return lista de notas en ese rango
     */
    List<Nota> findByUsuarioAndFechaCreacionBetweenOrderByFechaCreacionDesc(
            Usuario usuario,
            LocalDateTime inicio,
            LocalDateTime fin
    );

    /**
     * Cuenta el número de notas que tiene un usuario en cada fecha.
     * Usado para construir el heatmap de actividad del frontend.
     *
     * Devuelve una lista de Object[2] donde:
     *   [0] → fecha en formato "YYYY-MM-DD" (String)
     *   [1] → cantidad de notas ese día (Long)
     */
    @Query("""
            SELECT DATE_FORMAT(n.fechaCreacion, '%Y-%m-%d'), COUNT(n)
            FROM Nota n
            WHERE n.usuario = :usuario
            GROUP BY DATE_FORMAT(n.fechaCreacion, '%Y-%m-%d')
            ORDER BY DATE_FORMAT(n.fechaCreacion, '%Y-%m-%d') ASC
            """)
    List<Object[]> countNotasByDiaParaUsuario(@Param("usuario") Usuario usuario);
}
