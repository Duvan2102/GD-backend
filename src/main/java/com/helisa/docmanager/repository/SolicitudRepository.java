package com.helisa.docmanager.repository;


import com.helisa.docmanager.model.Estado;
import com.helisa.docmanager.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {

    // Búsquedas por creador
    Page<Solicitud> findByIdSolicitante(Integer idSolicitante, Pageable pageable);

    // Búsqueda por creador y estado (usando la entidad Estado)
    Page<Solicitud> findByIdSolicitanteAndEstado(
            Integer idSolicitante,
            Estado estado,
            Pageable pageable);

    // Búsqueda por tipología y estado
    Page<Solicitud> findByIdTipologiaAndEstado_DescripcionIgnoreCase(
            Integer idTipologia,
            String descripcion,
            Pageable pageable);

    // Solicitudes pendientes para gestionar
    @Query("SELECT DISTINCT s FROM Solicitud s JOIN s.destinatariosDetalle d WHERE s.estado.idEstado = 1 AND d.usuarioId = :usuarioId AND d.decision = 'PENDIENTE' AND (s.ordenFirmaBoolean = false OR (s.ordenFirmaBoolean = true AND d.ordenIndex = (SELECT MIN(d2.ordenIndex) FROM SolicitudDestinatario d2 WHERE d2.solicitud = s AND d2.decision = 'PENDIENTE')))")
    Page<Solicitud> findPendientesParaGestionar(
            @Param("usuarioId") Integer usuarioId,
            @Param("ordenSecuencial") boolean ordenSecuencial,
            Pageable pageable);

    // Histórico del usuario
    @Query("SELECT DISTINCT s FROM Solicitud s JOIN s.destinatariosDetalle d WHERE d.usuarioId = :usuarioId AND s.estado.idEstado IN (2, 3, 4)")
    Page<Solicitud> findHistoricoUsuario(
            @Param("usuarioId") Integer usuarioId,
            Pageable pageable);

    // Búsqueda por estado usando ID
    @Query("SELECT s FROM Solicitud s WHERE s.estado.idEstado = :estadoId")
    Page<Solicitud> findByEstadoId(@Param("estadoId") Integer estadoId, Pageable pageable);

    // Búsqueda por estado usando descripción
    @Query("SELECT s FROM Solicitud s WHERE s.estado.descripcion = :descripcion")
    Page<Solicitud> findByEstadoDescripcion(@Param("descripcion") String descripcion, Pageable pageable);

    // Contar por estado
    @Query("SELECT COUNT(s) FROM Solicitud s WHERE s.estado.idEstado = :estadoId")
    Long countByEstadoId(@Param("estadoId") Integer estadoId);

    boolean existsById(Integer id);
}