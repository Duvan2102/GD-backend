package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Solicitud;
import com.helisa.docmanager.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SolicitudService {
    
    @Autowired
    private SolicitudRepository solicitudRepository;
    
    public List<Solicitud> getAllSolicitudes() {
        return solicitudRepository.findAll();
    }
    
    public Optional<Solicitud> getSolicitudById(Integer id) {
        return solicitudRepository.findById(id);
    }
    
    public Solicitud saveSolicitud(Solicitud solicitud) {
        return solicitudRepository.save(solicitud);
    }
    
    public void deleteSolicitud(Integer id) {
        solicitudRepository.deleteById(id);
    }
}