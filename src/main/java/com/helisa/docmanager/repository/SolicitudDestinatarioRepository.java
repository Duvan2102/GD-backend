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

    // Buscar destinatario específico
    @Query("SELECT d FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId AND d.usuarioId = :usuarioId")
    Optional<SolicitudDestinatario> findBySolicitudIdAndUsuarioId(
            @Param("solicitudId") Integer solicitudId,
            @Param("usuarioId") Integer usuarioId);

    // Listar todos los destinatarios de una solicitud
    @Query("SELECT d FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId ORDER BY d.ordenIndex ASC")
    List<SolicitudDestinatario> findBySolicitudId(
            @Param("solicitudId") Integer solicitudId);

    // Buscar pendientes ordenados (para flujo secuencial)
    @Query("SELECT d FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId AND d.decision = 'PENDIENTE' ORDER BY d.ordenIndex ASC")
    List<SolicitudDestinatario> findPendientesBySolicitudId(
            @Param("solicitudId") Integer solicitudId);

    // Contar aprobados
    @Query("SELECT COUNT(d) FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId AND d.decision = 'APROBADO'")
    Long countAprobadosBySolicitudId(@Param("solicitudId") Integer solicitudId);

    // Contar total
    @Query("SELECT COUNT(d) FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId")
    Long countTotalBySolicitudId(@Param("solicitudId") Integer solicitudId);

    // Verificar si todos aprobaron
    @Query("SELECT CASE WHEN COUNT(d) = 0 THEN true ELSE false END FROM SolicitudDestinatario d WHERE d.solicitud.id = :solicitudId AND d.decision != 'APROBADO'")
    boolean todosAprobaron(@Param("solicitudId") Integer solicitudId);
}
