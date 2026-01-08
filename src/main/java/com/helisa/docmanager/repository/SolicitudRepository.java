package com.helisa.docmanager.repository;


import com.helisa.docmanager.model.Estado;
import com.helisa.docmanager.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {

    Page<Solicitud> findByIdSolicitante(Integer idSolicitante, Pageable pageable);

    Page<Solicitud> findByIdSolicitanteAndEstado(
            Integer idSolicitante,
            Estado estado,
            Pageable pageable);

    Page<Solicitud> findByIdTipologiaAndEstado_DescripcionIgnoreCase(
            Integer idTipologia,
            String descripcion,
            Pageable pageable);

    @Query("SELECT DISTINCT s FROM Solicitud s JOIN s.destinatariosDetalle d WHERE s.estado.idEstado = 1 AND d.usuarioId = :usuarioId AND d.decision = 'PENDIENTE' AND d.esProcesador = false AND (s.ordenFirmaBoolean = false OR (s.ordenFirmaBoolean = true AND d.ordenIndex = (SELECT MIN(d2.ordenIndex) FROM SolicitudDestinatario d2 WHERE d2.solicitud = s AND d2.decision = 'PENDIENTE' AND d2.esProcesador = false)))")
    Page<Solicitud> findPendientesParaGestionar(
            @Param("usuarioId") Integer usuarioId,
            @Param("ordenSecuencial") boolean ordenSecuencial,
            Pageable pageable);

    @Query("SELECT DISTINCT s FROM Solicitud s JOIN s.destinatariosDetalle d WHERE s.estado.idEstado = 9 AND d.usuarioId = :usuarioId AND d.decision = 'PENDIENTE' AND d.esProcesador = true AND d.ordenIndex = (SELECT MIN(d2.ordenIndex) FROM SolicitudDestinatario d2 WHERE d2.solicitud = s AND d2.decision = 'PENDIENTE' AND d2.esProcesador = true)")
    Page<Solicitud> findPendientesParaProcesar(
            @Param("usuarioId") Integer usuarioId,
            Pageable pageable);

    @Query("SELECT DISTINCT s FROM Solicitud s JOIN s.destinatariosDetalle d WHERE d.usuarioId = :usuarioId AND s.estado.idEstado IN (2, 3, 4)")
    Page<Solicitud> findHistoricoUsuario(
            @Param("usuarioId") Integer usuarioId,
            Pageable pageable);

    @Query("SELECT s FROM Solicitud s WHERE s.estado.idEstado = :estadoId")
    Page<Solicitud> findByEstadoId(@Param("estadoId") Integer estadoId, Pageable pageable);

    @Query("SELECT s FROM Solicitud s WHERE s.estado.descripcion = :descripcion")
    Page<Solicitud> findByEstadoDescripcion(@Param("descripcion") String descripcion, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Solicitud s WHERE s.estado.idEstado = :estadoId")
    Long countByEstadoId(@Param("estadoId") Integer estadoId);

    boolean existsById(Integer id);

    java.util.List<Solicitud> findByEstado_IdEstadoAndEnviarRecordatorioGreaterThan(Integer idEstado, Integer minValor);
    
    @Query("SELECT s FROM Solicitud s WHERE s.idTipologia IN :tipologiaIds")
    Page<Solicitud> findByIdTipologiaIn(@Param("tipologiaIds") java.util.List<Integer> tipologiaIds, Pageable pageable);
    
    Page<Solicitud> findByCreatedAtBetween(LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable);
    Page<Solicitud> findByCreatedAtAfter(LocalDateTime fechaDesde, Pageable pageable);
    Page<Solicitud> findByCreatedAtBefore(LocalDateTime fechaHasta, Pageable pageable);
    Page<Solicitud> findByEstado(Estado estado, Pageable pageable);
}