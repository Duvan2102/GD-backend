package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.SolicitudHistorial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudHistorialRepository extends JpaRepository<SolicitudHistorial, Long> {

    @Query("""
        SELECT h FROM SolicitudHistorial h
        WHERE h.solicitud.id = :solicitudId
        ORDER BY h.fecha DESC
        """)
    List<SolicitudHistorial> findBySolicitudId(@Param("solicitudId") Integer solicitudId);

    @Query("""
        SELECT h FROM SolicitudHistorial h
        WHERE h.solicitud.id = :solicitudId
        ORDER BY h.fecha DESC
        """)
    Page<SolicitudHistorial> findBySolicitudIdPaged(
            @Param("solicitudId") Integer solicitudId,
            Pageable pageable);

    @Query("""
        SELECT h FROM SolicitudHistorial h
        WHERE h.actorUsuarioId = :usuarioId
        ORDER BY h.fecha DESC
        """)
    Page<SolicitudHistorial> findByActorUsuarioId(
            @Param("usuarioId") Integer usuarioId,
            Pageable pageable);

	SolicitudHistorial findFirstBySolicitudIdOrderByFechaAsc(Long solicitudId);



}
