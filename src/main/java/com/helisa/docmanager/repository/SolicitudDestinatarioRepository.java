package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.SolicitudDestinatario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudDestinatarioRepository extends JpaRepository<SolicitudDestinatario, Long> {

    @Query("SELECT d FROM SolicitudDestinatario d " +
            "WHERE d.solicitud.id = :solicitudId " +
            "AND d.usuarioId = :usuarioId")
    Optional<SolicitudDestinatario> findBySolicitudIdAndUsuarioId(@Param("solicitudId") Integer solicitudId,
                                                                  @Param("usuarioId") Integer usuarioId);

    @Query("SELECT d FROM SolicitudDestinatario d " +
            "WHERE d.solicitud.id = :solicitudId " +
            "AND d.decision = 'PENDIENTE' " +
            "ORDER BY d.ordenIndex")
    List<SolicitudDestinatario> findPendientesBySolicitudId(@Param("solicitudId") Integer solicitudId);

    @Query("SELECT COUNT(d) FROM SolicitudDestinatario d " +
            "WHERE d.solicitud.id = :solicitudId " +
            "AND d.decision = 'APROBADO'")
    Long countAprobadosBySolicitudId(@Param("solicitudId") Integer solicitudId);

    @Query("SELECT COUNT(d) FROM SolicitudDestinatario d " +
            "WHERE d.solicitud.id = :solicitudId")
    Long countTotalBySolicitudId(@Param("solicitudId") Integer solicitudId);

    @Query("SELECT d FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId")
    List<SolicitudDestinatario> findBySolicitudId(@Param("solicitudId") Integer solicitudId);
}
