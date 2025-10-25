package com.helisa.docmanager.controller;

import com.helisa.docmanager.dto.AuditFiltersRequest;
import com.helisa.docmanager.dto.AuditSolicitudResponse;
import com.helisa.docmanager.dto.ExcelAuditRequest;
import com.helisa.docmanager.service.AuditService;
import com.helisa.docmanager.service.ExcelAuditService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private ExcelAuditService excelAuditService;
    
    /**
     * Endpoint para consultar solicitudes con filtros de auditoría
     * POST /api/audit/solicitudes
     */
    @PostMapping("/solicitudes")
    public ResponseEntity<List<AuditSolicitudResponse>> getSolicitudesWithFilters(
            @Valid @RequestBody AuditFiltersRequest request) {
        try {
            List<AuditSolicitudResponse> solicitudes = auditService.getSolicitudesWithFilters(request);
            return ResponseEntity.ok(solicitudes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Endpoint para generar y descargar el reporte de auditoría en Excel
     * POST /api/audit/excel
     */
    @PostMapping("/excel")
    public ResponseEntity<byte[]> generateExcelAuditReport(
            @Valid @RequestBody ExcelAuditRequest request) {
        try {
            byte[] excelBytes = excelAuditService.generateExcelAuditReport(request);
            
            // Generar nombre de archivo con timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "auditoria_solicitudes_" + timestamp + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
                    
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Endpoint de salud para verificar que el servicio de auditoría está funcionando
     * GET /api/audit/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Servicio de auditoría funcionando correctamente");
    }
}