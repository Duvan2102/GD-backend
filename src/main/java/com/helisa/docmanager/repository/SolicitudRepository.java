package com.helisa.docmanager.repository;


import com.helisa.docmanager.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    Page<Solicitud> findByIdSolicitanteAndEstado(Long idSolicitante,
                                                 Solicitud.EstadoSolicitud estado,
                                                 Pageable pageable);

    Page<Solicitud> findByIdSolicitante(Long idSolicitante, Pageable pageable);

    @Query("SELECT s FROM Solicitud s " +
            "JOIN s.destinatarios d " +
            "WHERE d.usuarioId = :usuarioId " +
            "AND d.decision = 'PENDIENTE' " +
            "AND s.estado = 'PENDIENTE' " +
            "AND (:ordenSecuencial = false OR " +
            "     d.ordenIndex = (SELECT MIN(d2.ordenIndex) FROM SolicitudDestinatario d2 " +
            "                     WHERE d2.solicitud = s AND d2.decision = 'PENDIENTE'))")
    Page<Solicitud> findPendientesParaGestionar(@Param("usuarioId") Long usuarioId,
                                                @Param("ordenSecuencial") boolean ordenSecuencial,
                                                Pageable pageable);

    @Query("SELECT s FROM Solicitud s " +
            "JOIN s.destinatarios d " +
            "WHERE d.usuarioId = :usuarioId " +
            "AND d.decision != 'PENDIENTE'")
    Page<Solicitud> findHistoricoUsuario(@Param("usuarioId") Long usuarioId, Pageable pageable);

    @Query("SELECT s FROM Solicitud s " +
            "WHERE s.tipologiaId = :tipologiaId " +
            "AND s.estado = :estado")
    Page<Solicitud> findByTipologiaIdAndEstado(@Param("tipologiaId") Long tipologiaId,
                                               @Param("estado") Solicitud.EstadoSolicitud estado,
                                               Pageable pageable);
}
