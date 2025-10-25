package com.helisa.docmanager.controller;

import com.helisa.docmanager.model.Solicitud;
import com.helisa.docmanager.service.SolicitudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/solicitudes")
@CrossOrigin(origins = "*")
public class SolicitudController {
    
    @Autowired
    private SolicitudService solicitudService;
    
    @GetMapping
    public ResponseEntity<List<Solicitud>> getAllSolicitudes() {
        List<Solicitud> solicitudes = solicitudService.getAllSolicitudes();
        return ResponseEntity.ok(solicitudes);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Solicitud> getSolicitudById(@PathVariable Integer id) {
        Optional<Solicitud> solicitud = solicitudService.getSolicitudById(id);
        return solicitud.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Solicitud> createSolicitud(@RequestBody Solicitud solicitud) {
        Solicitud savedSolicitud = solicitudService.saveSolicitud(solicitud);
        return ResponseEntity.ok(savedSolicitud);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Solicitud> updateSolicitud(@PathVariable Integer id, @RequestBody Solicitud solicitud) {
        if (solicitudService.getSolicitudById(id).isPresent()) {
            solicitud.setIdSolicitud(id);
            Solicitud updatedSolicitud = solicitudService.saveSolicitud(solicitud);
            return ResponseEntity.ok(updatedSolicitud);
        }
        return ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSolicitud(@PathVariable Integer id) {
        if (solicitudService.getSolicitudById(id).isPresent()) {
            solicitudService.deleteSolicitud(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}