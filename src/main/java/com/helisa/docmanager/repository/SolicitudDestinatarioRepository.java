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

    @Query("SELECT d FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId AND d.usuarioId = :usuarioId")
    Optional<SolicitudDestinatario> findBySolicitudIdAndUsuarioId(
            @Param("solicitudId") Integer solicitudId,
            @Param("usuarioId") Integer usuarioId);

    @Query("SELECT d FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId AND d.usuarioId = :usuarioId AND d.esProcesador = :esProcesador")
    Optional<SolicitudDestinatario> findBySolicitudIdAndUsuarioIdAndEsProcesador(
            @Param("solicitudId") Integer solicitudId,
            @Param("usuarioId") Integer usuarioId,
            @Param("esProcesador") Boolean esProcesador);

    @Query("SELECT d FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId ORDER BY d.ordenIndex ASC")
    List<SolicitudDestinatario> findBySolicitudId(
            @Param("solicitudId") Integer solicitudId);

    @Query("SELECT d FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId AND d.decision = 'PENDIENTE' AND d.esProcesador = :esProcesador ORDER BY d.ordenIndex ASC")
    List<SolicitudDestinatario> findPendientesBySolicitudId(
            @Param("solicitudId") Integer solicitudId,
            @Param("esProcesador") Boolean esProcesador);

    @Query("SELECT COUNT(d) FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId AND d.decision = 'APROBADO' AND d.esProcesador = :esProcesador")
    Long countAprobadosBySolicitudId(
            @Param("solicitudId") Integer solicitudId,
            @Param("esProcesador") Boolean esProcesador);

    @Query("SELECT COUNT(d) FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId AND d.esProcesador = :esProcesador")
    Long countTotalBySolicitudId(
            @Param("solicitudId") Integer solicitudId,
            @Param("esProcesador") Boolean esProcesador);

    @Query("SELECT CASE WHEN COUNT(d) = 0 THEN true ELSE false END FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId AND d.decision != 'APROBADO' AND d.esProcesador = :esProcesador")
    boolean todosAprobaron(
            @Param("solicitudId") Integer solicitudId,
            @Param("esProcesador") Boolean esProcesador);

    @Query("SELECT d FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId AND d.decision = 'PENDIENTE' AND d.esProcesador = true ORDER BY d.ordenIndex ASC")
    List<SolicitudDestinatario> findPendientesProcesadoresBySolicitudId(
            @Param("solicitudId") Integer solicitudId);

    @Query("SELECT COUNT(d) > 0 FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId AND d.usuarioId = :usuarioId")
    boolean existsBySolicitudIdAndUsuarioId(
            @Param("solicitudId") Integer solicitudId,
            @Param("usuarioId") Integer usuarioId);
}
