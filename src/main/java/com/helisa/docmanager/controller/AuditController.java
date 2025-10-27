package com.helisa.docmanager.controller;

import com.helisa.docmanager.dto.response.AuditSolicitudResponse;
import com.helisa.docmanager.model.AuditExcelRequest;
import com.helisa.docmanager.model.AuditSearchRequest;
import com.helisa.docmanager.service.AuditService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/auditoria")
@Slf4j
public class AuditController {

    @Autowired
    private AuditService auditService;

    /**
     * Busca solicitudes con filtros de auditoría
     * POST /api/auditoria/buscar
     */
    @PostMapping("/buscar")
    public ResponseEntity<Page<AuditSolicitudResponse>> buscarSolicitudes(
            @Valid @RequestBody AuditSearchRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Buscando solicitudes de auditoría - CorrelationId: {}, Filtros: {}", 
                correlationId, request.getFilters());

        Page<AuditSolicitudResponse> response = auditService.buscarSolicitudesAuditoria(request, pageable);

        log.info("Búsqueda de auditoría completada - CorrelationId: {}, Total: {}", 
                correlationId, response.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * Genera Excel de auditoría para las solicitudes seleccionadas
     * POST /api/auditoria/excel
     */
    @PostMapping("/excel")
    public ResponseEntity<Resource> generarExcelAuditoria(
            @Valid @RequestBody AuditExcelRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Generando Excel de auditoría - CorrelationId: {}, Solicitudes: {}", 
                correlationId, request.getSelectedIds().size());

        try {
            byte[] excelBytes = auditService.generarExcelAuditoria(request);
            
            // Generar nombre de archivo con timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "auditoria_solicitudes_" + timestamp + ".xlsx";

            ByteArrayResource resource = new ByteArrayResource(excelBytes);

            log.info("Excel de auditoría generado exitosamente - CorrelationId: {}, Archivo: {}", 
                    correlationId, filename);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentLength(excelBytes.length)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error generando Excel de auditoría - CorrelationId: {}", correlationId, e);
            throw new RuntimeException("Error generando Excel de auditoría: " + e.getMessage(), e);
        }
    }
}