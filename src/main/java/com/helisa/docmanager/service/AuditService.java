package com.helisa.docmanager.service;

import com.helisa.docmanager.dto.AuditFiltersRequest;
import com.helisa.docmanager.dto.AuditSolicitudResponse;
import com.helisa.docmanager.model.Solicitud;
import com.helisa.docmanager.repository.SolicitudAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {
    
    @Autowired
    private SolicitudAuditRepository solicitudAuditRepository;
    
    public List<AuditSolicitudResponse> getSolicitudesWithFilters(AuditFiltersRequest request) {
        // Validar que al menos un filtro esté presente
        validateFilters(request);
        
        // Convertir fechas a LocalDateTime
        LocalDateTime fechaDesde = null;
        LocalDateTime fechaHasta = null;
        
        if (request.getFilters().getFechas() != null) {
            if (request.getFilters().getFechas().getFechaDesde() != null) {
                fechaDesde = request.getFilters().getFechas().getFechaDesde().atStartOfDay();
            }
            if (request.getFilters().getFechas().getFechaHasta() != null) {
                fechaHasta = request.getFilters().getFechas().getFechaHasta().atTime(LocalTime.MAX);
            }
        }
        
        // Obtener solicitudes filtradas
        List<Solicitud> solicitudes = solicitudAuditRepository.findSolicitudesWithFilters(
                fechaDesde,
                fechaHasta,
                request.getFilters().getSolicitante(),
                request.getFilters().getEstado(),
                request.getFilters().getTipologia(),
                request.getFilters().getDepartamento()
        );
        
        // Convertir a DTOs de respuesta
        return solicitudes.stream()
                .map(this::convertToAuditResponse)
                .collect(Collectors.toList());
    }
    
    public List<AuditSolicitudResponse> getSolicitudesForExcel(List<String> selectedIds) {
        // Convertir IDs de String a Integer
        List<Integer> ids = selectedIds.stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        
        // Obtener solicitudes con historial
        List<Solicitud> solicitudes = solicitudAuditRepository.findSolicitudesWithHistorialByIds(ids);
        
        // Convertir a DTOs de respuesta
        return solicitudes.stream()
                .map(this::convertToAuditResponseWithHistorial)
                .collect(Collectors.toList());
    }
    
    private void validateFilters(AuditFiltersRequest request) {
        boolean hasAnyFilter = false;
        
        if (request.getFilters().getFechas() != null) {
            if (request.getFilters().getFechas().getFechaDesde() != null || 
                request.getFilters().getFechas().getFechaHasta() != null) {
                hasAnyFilter = true;
            }
        }
        
        if (request.getFilters().getSolicitante() != null && !request.getFilters().getSolicitante().trim().isEmpty()) {
            hasAnyFilter = true;
        }
        
        if (request.getFilters().getEstado() != null && !request.getFilters().getEstado().trim().isEmpty()) {
            hasAnyFilter = true;
        }
        
        if (request.getFilters().getTipologia() != null && !request.getFilters().getTipologia().trim().isEmpty()) {
            hasAnyFilter = true;
        }
        
        if (request.getFilters().getDepartamento() != null && !request.getFilters().getDepartamento().trim().isEmpty()) {
            hasAnyFilter = true;
        }
        
        if (!hasAnyFilter) {
            throw new IllegalArgumentException("Debe proporcionar al menos un filtro para realizar la consulta de auditoría");
        }
    }
    
    private AuditSolicitudResponse convertToAuditResponse(Solicitud solicitud) {
        AuditSolicitudResponse response = new AuditSolicitudResponse();
        
        response.setIdSolicitud(solicitud.getIdSolicitud());
        response.setNombreSolicitud(solicitud.getNombreSolicitud());
        response.setFechaRegistro(solicitud.getFechaRegistro());
        response.setDetallesAdicionales(solicitud.getDetallesAdicionales());
        response.setPrioridad(solicitud.getPrioridad());
        response.setEnviarRecordatorio(solicitud.getEnviarRecordatorio());
        response.setDocumentoPrincipal(solicitud.getDocumentoPrincipal());
        response.setDocumentosAdicionales(solicitud.getDocumentosAdicionales());
        response.setDestinatarios(solicitud.getDestinatarios());
        response.setOrdenFirma(solicitud.getOrdenFirma());
        
        // Información de tipología
        if (solicitud.getTipologia() != null) {
            response.setTipologia(solicitud.getTipologia().getDescripcion());
        }
        
        // Información de estado
        if (solicitud.getEstado() != null) {
            response.setEstado(solicitud.getEstado().getDescripcion());
        }
        
        // Información del solicitante
        if (solicitud.getSolicitante() != null) {
            response.setSolicitante(solicitud.getSolicitante().getNombres() + " " + solicitud.getSolicitante().getApellidos());
            
            // Información de departamento, área y cargo
            if (solicitud.getSolicitante().getCargo() != null) {
                response.setCargo(solicitud.getSolicitante().getCargo().getDescripcion());
                
                if (solicitud.getSolicitante().getCargo().getArea() != null) {
                    response.setArea(solicitud.getSolicitante().getCargo().getArea().getDescripcion());
                    
                    if (solicitud.getSolicitante().getCargo().getArea().getDepartamento() != null) {
                        response.setDepartamento(solicitud.getSolicitante().getCargo().getArea().getDepartamento().getDescripcion());
                    }
                }
            }
        }
        
        return response;
    }
    
    private AuditSolicitudResponse convertToAuditResponseWithHistorial(Solicitud solicitud) {
        AuditSolicitudResponse response = convertToAuditResponse(solicitud);
        
        // Agregar historial de comentarios
        if (solicitud.getComentarios() != null) {
            List<AuditSolicitudResponse.HistorialComentario> historial = solicitud.getComentarios().stream()
                    .map(comentario -> {
                        AuditSolicitudResponse.HistorialComentario hist = new AuditSolicitudResponse.HistorialComentario();
                        hist.setIdComentario(comentario.getIdComentario());
                        hist.setDescripcion(comentario.getDescripcion());
                        hist.setUsuario(comentario.getUsuario().getNombres() + " " + comentario.getUsuario().getApellidos());
                        hist.setFechaRegistro(comentario.getFechaRegistro());
                        return hist;
                    })
                    .collect(Collectors.toList());
            response.setHistorial(historial);
        }
        
        return response;
    }
}