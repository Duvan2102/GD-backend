package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SolicitudAuditRepository extends JpaRepository<Solicitud, Integer> {
    
    @Query("SELECT s FROM Solicitud s " +
           "LEFT JOIN s.tipologia t " +
           "LEFT JOIN s.estado e " +
           "LEFT JOIN s.solicitante sol " +
           "LEFT JOIN sol.cargo c " +
           "LEFT JOIN c.area a " +
           "LEFT JOIN a.departamento d " +
           "WHERE " +
           "(:fechaDesde IS NULL OR s.fechaRegistro >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR s.fechaRegistro <= :fechaHasta) AND " +
           "(:solicitante IS NULL OR LOWER(CONCAT(sol.nombres, ' ', sol.apellidos)) LIKE LOWER(CONCAT('%', :solicitante, '%'))) AND " +
           "(:estado IS NULL OR LOWER(e.descripcion) = LOWER(:estado)) AND " +
           "(:tipologia IS NULL OR LOWER(t.descripcion) = LOWER(:tipologia)) AND " +
           "(:departamento IS NULL OR LOWER(d.descripcion) = LOWER(:departamento)) " +
           "ORDER BY s.fechaRegistro DESC")
    List<Solicitud> findSolicitudesWithFilters(
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("solicitante") String solicitante,
            @Param("estado") String estado,
            @Param("tipologia") String tipologia,
            @Param("departamento") String departamento
    );
    
    @Query("SELECT s FROM Solicitud s " +
           "LEFT JOIN FETCH s.tipologia t " +
           "LEFT JOIN FETCH s.estado e " +
           "LEFT JOIN FETCH s.solicitante sol " +
           "LEFT JOIN FETCH sol.cargo c " +
           "LEFT JOIN FETCH c.area a " +
           "LEFT JOIN FETCH a.departamento d " +
           "LEFT JOIN FETCH s.comentarios com " +
           "LEFT JOIN FETCH com.usuario u " +
           "WHERE s.idSolicitud IN :ids " +
           "ORDER BY s.fechaRegistro DESC, com.fechaRegistro ASC")
    List<Solicitud> findSolicitudesWithHistorialByIds(@Param("ids") List<Integer> ids);
}