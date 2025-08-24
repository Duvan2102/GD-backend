package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.SolicitudAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudAdjuntoRepository extends JpaRepository<SolicitudAdjunto, Long> {

    List<SolicitudAdjunto> findBySolicitudId(Long solicitudId);
}
