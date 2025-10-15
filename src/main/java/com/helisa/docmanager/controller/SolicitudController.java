package com.helisa.docmanager.controller;

import com.helisa.docmanager.dto.response.AdjuntoResponse;
import com.helisa.docmanager.dto.response.SolicitudDetalleResponse;
import com.helisa.docmanager.dto.response.SolicitudResumenResponse;
import com.helisa.docmanager.model.CrearSolicitudRequest;
import com.helisa.docmanager.model.DecisionRequest;
import com.helisa.docmanager.service.SolicitudService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
@Slf4j
public class SolicitudController {

    @Autowired
    private SolicitudService solicitudService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SolicitudDetalleResponse> crear(
            @Valid @ModelAttribute CrearSolicitudRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Creando solicitud - CorrelationId: {}, Solicitante: {}", 
                correlationId, request.getIdSolicitante());

        SolicitudDetalleResponse response = solicitudService.crear(request);

        log.info("Solicitud creada exitosamente - ID: {}, CorrelationId: {}",
                response.getId(), correlationId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{solicitudId}/descargas")
    public ResponseEntity<Void> registrarDescarga(
            @PathVariable("solicitudId") Integer solicitudId,
            @RequestParam("usuarioId") Integer usuarioId,
            @RequestParam(value = "tipoDescarga", defaultValue = "DESCARGAR") String tipoDescarga,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Registrar descarga - CorrelationId: {}, Solicitud: {}, Usuario: {}, Tipo: {}",
                correlationId, solicitudId, usuarioId, tipoDescarga);
        solicitudService.registrarDescarga(solicitudId, usuarioId, tipoDescarga);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudDetalleResponse> obtenerDetalle(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Consultando solicitud - ID: {}, CorrelationId: {}", id, correlationId);
        SolicitudDetalleResponse response = solicitudService.obtenerDetalle(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<Void> aprobar(
            @PathVariable Integer id,
            @Valid @RequestBody DecisionRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Aprobando solicitud - ID: {}, Usuario: {}, CorrelationId: {}",
                id, request.getUsuarioId(), correlationId);
        solicitudService.aprobar(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<Void> rechazar(
            @PathVariable Integer id,
            @Valid @RequestBody DecisionRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Rechazando solicitud - ID: {}, Usuario: {}, CorrelationId: {}",
                id, request.getUsuarioId(), correlationId);
        solicitudService.rechazar(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(
            @PathVariable Integer id,
            @Valid @RequestBody DecisionRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Cancelando solicitud - ID: {}, Usuario: {}, CorrelationId: {}",
                id, request.getUsuarioId(), correlationId);
        solicitudService.cancelar(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<InputStreamResource> streamPdf(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        try {
            log.info("Streaming PDF - Solicitud: {}, CorrelationId: {}", id, correlationId);

            InputStream pdfStream = solicitudService.streamPdfPrincipal(id);
            String filename = solicitudService.obtenerNombrePdf(id);

            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + encodedFilename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(new InputStreamResource(pdfStream));

        } catch (Exception e) {
            log.error("Error al stream PDF de solicitud {}", id, e);
            throw new RuntimeException("Error al obtener PDF");
        }
    }

    @GetMapping("/{id}/adjuntos")
    public ResponseEntity<List<AdjuntoResponse>> listarAdjuntos(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Listando adjuntos - Solicitud: {}, CorrelationId: {}", id, correlationId);
        List<AdjuntoResponse> adjuntos = solicitudService.listarAdjuntos(id);
        return ResponseEntity.ok(adjuntos);
    }

    @GetMapping("/{id}/adjuntos/{adjuntoId}/download")
    public ResponseEntity<InputStreamResource> descargarAdjunto(
            @PathVariable Integer id,
            @PathVariable Long adjuntoId,  // Long porque SolicitudAdjunto usa Long como ID
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        try {
            log.info("Descargando adjunto - Solicitud: {}, Adjunto: {}, CorrelationId: {}",
                    id, adjuntoId, correlationId);

            InputStream adjuntoStream = solicitudService.descargarAdjunto(id, adjuntoId);
            String filename = solicitudService.obtenerNombreAdjunto(adjuntoId);

            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encodedFilename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(new InputStreamResource(adjuntoStream));

        } catch (Exception e) {
            log.error("Error al descargar adjunto {} de solicitud {}", adjuntoId, id, e);
            throw new RuntimeException("Error al descargar adjunto");
        }
    }

    @GetMapping("/{id}/descargar-todo")
    public ResponseEntity<InputStreamResource> descargarTodo(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        try {
            log.info("Descargando todo (ZIP) - Solicitud: {}, CorrelationId: {}", id, correlationId);
            InputStream zipStream = solicitudService.descargarTodoComoZip(id);
            String zipName = "solicitud-" + id + ".zip";
            String encodedFilename = URLEncoder.encode(zipName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(zipStream));
        } catch (Exception e) {
            log.error("Error al descargar ZIP de solicitud {}", id, e);
            throw new RuntimeException("Error al descargar todo");
        }
    }

    // =============== ENDPOINTS DE LISTADO ===============

    @GetMapping
    public ResponseEntity<Page<SolicitudResumenResponse>> listarPorCreador(
            @RequestParam Integer creadorId,
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Listando solicitudes por creador - Creador: {}, Estado: {}, CorrelationId: {}",
                creadorId, estado, correlationId);

        Page<SolicitudResumenResponse> solicitudes =
                solicitudService.listarPorCreador(creadorId, estado, pageable);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/para-gestionar")
    public ResponseEntity<Page<SolicitudResumenResponse>> listarParaGestionar(
            @RequestParam Integer usuarioId,  // Corregido a Integer
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Listando solicitudes para gestionar - Usuario: {}, CorrelationId: {}",
                usuarioId, correlationId);

        Page<SolicitudResumenResponse> solicitudes =
                solicitudService.listarParaGestionar(usuarioId, pageable);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/historico")
    public ResponseEntity<Page<SolicitudResumenResponse>> listarHistorico(
            @RequestParam Integer usuarioId,  // Corregido a Integer
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Listando histórico - Usuario: {}, CorrelationId: {}", usuarioId, correlationId);

        Page<SolicitudResumenResponse> solicitudes =
                solicitudService.listarHistorico(usuarioId, pageable);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/finalizadas")
    public ResponseEntity<Page<SolicitudResumenResponse>> listarFinalizadas(
            @RequestParam Integer tipologiaId,
            @RequestParam(defaultValue = "APROBADO") String estado,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Listando finalizadas - Tipología: {}, Estado: {}, CorrelationId: {}",
                tipologiaId, estado, correlationId);

        Page<SolicitudResumenResponse> solicitudes =
                solicitudService.listarFinalizadas(tipologiaId, estado, pageable);
        return ResponseEntity.ok(solicitudes);
    }
}