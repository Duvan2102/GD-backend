package com.helisa.docmanager.controller;

import com.helisa.docmanager.dto.response.AdjuntoResponse;
import com.helisa.docmanager.dto.response.SolicitudDetalleResponse;
import com.helisa.docmanager.dto.response.SolicitudResumenResponse;
import com.helisa.docmanager.model.CrearSolicitudRequest;
import com.helisa.docmanager.model.DecisionRequest;
import com.helisa.docmanager.model.ProcesadoresRequest;
import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.repository.UsuarioRepository;
import com.helisa.docmanager.service.SolicitudService;
import com.helisa.docmanager.service.TwoFactorAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
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
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    @Autowired
    private UsuarioRepository usuarioRepository;

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
    @PostMapping("/{id}/agregar-procesadores")
    public ResponseEntity<Void> agregarProcesadores(
            @PathVariable Integer id,
            @Valid @RequestBody ProcesadoresRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Agregando procesadores a la solicitud - ID: {}, Procesadores: {}, CorrelationId: {}",
                id, request.getProcesadores(), correlationId);

        solicitudService.agregarProcesadores(id, request);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/{id}/validar-2fa")
    public ResponseEntity<?> validar2FA(
            @PathVariable Integer id,
            @Valid @RequestBody Validar2FARequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Validando 2FA para solicitud - ID: {}, Usuario: {}, CorrelationId: {}",
                id, request.getUsuarioId(), correlationId);

        try {
            Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (!twoFactorAuthService.validateTwoFactorCode(usuario, request.getCodigo2FA())) {
                String metodo = (usuario.getTokenQr() != null && usuario.getTokenQr()) 
                        ? "Google Authenticator" 
                        : "email";
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("CODIGO_2FA_INVALIDO", 
                                "Código de " + metodo + " incorrecto"));
            }

            log.info("2FA validado exitosamente - Solicitud: {}, Usuario: {}, CorrelationId: {}",
                    id, request.getUsuarioId(), correlationId);

            return ResponseEntity.ok(new Validar2FAResponse(
                    true, 
                    "Código 2FA validado correctamente. Puede proceder con la acción."));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("excedido el límite")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new ErrorResponse("LIMITE_EXCEDIDO", e.getMessage()));
            } else if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("USUARIO_NO_ENCONTRADO", e.getMessage()));
            } else if (e.getMessage().contains("no tiene método")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("2FA_NO_CONFIGURADO", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_VALIDACION", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al validar 2FA - Solicitud: {}, Usuario: {}", 
                    id, request.getUsuarioId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_INTERNO", 
                            "Error al validar código 2FA: " + e.getMessage()));
        }
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

    @PostMapping(value = "/{id}/adjuntos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<AdjuntoResponse>> agregarAdjuntos(
            @PathVariable Integer id,
            @RequestParam("archivos") MultipartFile[] archivos,
            @RequestParam("usuarioId") Integer usuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Agregando adjuntos a solicitud - ID: {}, Archivos: {}, Usuario: {}, CorrelationId: {}",
                id, archivos != null ? archivos.length : 0, usuarioId, correlationId);

        List<AdjuntoResponse> adjuntos = solicitudService.agregarAdjuntos(id, archivos, usuarioId);
        return ResponseEntity.ok(adjuntos);
    }

    @GetMapping("/{id}/adjuntos/{adjuntoId}/download")
    public ResponseEntity<InputStreamResource> descargarAdjunto(
            @PathVariable Integer id,
            @PathVariable Long adjuntoId, 
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
            @RequestParam Integer usuarioId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Listando solicitudes para gestionar - Usuario: {}, CorrelationId: {}",
                usuarioId, correlationId);

        Page<SolicitudResumenResponse> solicitudes =
                solicitudService.listarParaGestionar(usuarioId, pageable);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/para-procesar")
    public ResponseEntity<Page<SolicitudResumenResponse>> listarParaProcesar(
            @RequestParam Integer usuarioId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Listando solicitudes para procesar - Usuario: {}, CorrelationId: {}",
                usuarioId, correlationId);

        Page<SolicitudResumenResponse> solicitudes =
                solicitudService.listarParaProcesar(usuarioId, pageable);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/historico")
    public ResponseEntity<Page<SolicitudResumenResponse>> listarHistorico(
            @RequestParam Integer usuarioId,
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

    @GetMapping("/por-area")
    public ResponseEntity<Page<SolicitudResumenResponse>> listarPorArea(
            @RequestParam Integer usuarioId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        log.info("Listando solicitudes por área - Usuario: {}, CorrelationId: {}",
                usuarioId, correlationId);

        Page<SolicitudResumenResponse> solicitudes =
                solicitudService.listarPorArea(usuarioId, pageable);
        return ResponseEntity.ok(solicitudes);
    }

    // ========== CLASES DE REQUEST/RESPONSE ==========

    @Data
    public static class Validar2FARequest {
        @NotNull(message = "ID del usuario es obligatorio")
        @Positive(message = "ID del usuario debe ser positivo")
        private Integer usuarioId;

        @NotBlank(message = "El código 2FA es obligatorio")
        private String codigo2FA;
    }

    @Data
    public static class Validar2FAResponse {
        private final boolean valido;
        private final String message;

        public Validar2FAResponse(boolean valido, String message) {
            this.valido = valido;
            this.message = message;
        }
    }

    @Data
    public static class ErrorResponse {
        private final String code;
        private final String message;
    }
}