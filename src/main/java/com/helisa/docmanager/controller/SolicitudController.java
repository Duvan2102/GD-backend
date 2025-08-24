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
import java.util.List;

@RestController
@RequestMapping("/solicitudes")
@Slf4j
public class SolicitudController {

    @Autowired
    private SolicitudService solicitudService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SolicitudDetalleResponse> crear(
            @Valid @ModelAttribute CrearSolicitudRequest request) {

        SolicitudDetalleResponse response = solicitudService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudDetalleResponse> obtenerDetalle(@PathVariable Long id) {
        SolicitudDetalleResponse response = solicitudService.obtenerDetalle(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<Void> aprobar(@PathVariable Long id,
                                        @Valid @RequestBody DecisionRequest request) {
        solicitudService.aprobar(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<Void> rechazar(@PathVariable Long id,
                                         @Valid @RequestBody DecisionRequest request) {
        solicitudService.rechazar(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id,
                                         @Valid @RequestBody DecisionRequest request) {
        solicitudService.cancelar(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<InputStreamResource> streamPdf(@PathVariable Long id) {
        try {
            InputStream pdfStream = solicitudService.streamPdfPrincipal(id);
            String filename = solicitudService.obtenerNombrePdf(id);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(new InputStreamResource(pdfStream));

        } catch (Exception e) {
            log.error("Error al stream PDF de solicitud {}", id, e);
            throw new RuntimeException("Error al obtener PDF");
        }
    }
    // Más endpoints según especificación...
    @GetMapping("/{id}/adjuntos")
    public ResponseEntity<List<AdjuntoResponse>> listarAdjuntos(@PathVariable Long id) {
        List<AdjuntoResponse> adjuntos = solicitudService.listarAdjuntos(id);
        return ResponseEntity.ok(adjuntos);
    }

    @GetMapping("/{id}/adjuntos/{adjuntoId}/download")
    public ResponseEntity<InputStreamResource> descargarAdjunto(
            @PathVariable Long id, @PathVariable Long adjuntoId) {

        try {
            InputStream adjuntoStream = solicitudService.descargarAdjunto(id, adjuntoId);
            String filename = solicitudService.obtenerNombreAdjunto(adjuntoId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(new InputStreamResource(adjuntoStream));

        } catch (Exception e) {
            log.error("Error al descargar adjunto {} de solicitud {}", adjuntoId, id, e);
            throw new RuntimeException("Error al descargar adjunto");
        }
    }

    // Endpoints de listado
    @GetMapping
    public ResponseEntity<Page<SolicitudResumenResponse>> listarPorCreador(
            @RequestParam Long creadorId,
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<SolicitudResumenResponse> solicitudes =
                solicitudService.listarPorCreador(creadorId, estado, pageable);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/para-gestionar")
    public ResponseEntity<Page<SolicitudResumenResponse>> listarParaGestionar(
            @RequestParam Long usuarioId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<SolicitudResumenResponse> solicitudes =
                solicitudService.listarParaGestionar(usuarioId, pageable);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/historico")
    public ResponseEntity<Page<SolicitudResumenResponse>> listarHistorico(
            @RequestParam Long usuarioId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<SolicitudResumenResponse> solicitudes =
                solicitudService.listarHistorico(usuarioId, pageable);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/finalizadas")
    public ResponseEntity<Page<SolicitudResumenResponse>> listarFinalizadas(
            @RequestParam Long tipologiaId,
            @RequestParam(defaultValue = "APROBADO") String estado,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<SolicitudResumenResponse> solicitudes =
                solicitudService.listarFinalizadas(tipologiaId, estado, pageable);
        return ResponseEntity.ok(solicitudes);
    }
}
