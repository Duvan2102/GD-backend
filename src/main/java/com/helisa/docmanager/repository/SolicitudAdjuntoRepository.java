package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.SolicitudAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudAdjuntoRepository extends JpaRepository<SolicitudAdjunto, Long> {

    @Query("SELECT a FROM SolicitudAdjunto a WHERE a.solicitud.id = :solicitudId ORDER BY a.createdAt ASC")
    List<SolicitudAdjunto> findBySolicitudId(@Param("solicitudId") Integer solicitudId);

    @Query("SELECT COUNT(a) FROM SolicitudAdjunto a WHERE a.solicitud.id = :solicitudId")
    Long countBySolicitudId(@Param("solicitudId") Integer solicitudId);

    @Query("SELECT a FROM SolicitudAdjunto a WHERE a.id = :adjuntoId AND a.solicitud.id = :solicitudId")
    Optional<SolicitudAdjunto> findByIdAndSolicitudId(
            @Param("adjuntoId") Long adjuntoId,
            @Param("solicitudId") Integer solicitudId);
}

