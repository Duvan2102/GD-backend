package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.SolicitudHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudHistorialRepository extends JpaRepository<SolicitudHistorial, Long> {

    List<SolicitudHistorial> findBySolicitudIdOrderByFechaDesc(Long solicitudId);
}
