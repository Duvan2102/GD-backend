package com.helisa.docmanager.service;

import com.helisa.docmanager.dto.response.AdjuntoResponse;
import com.helisa.docmanager.dto.response.DestinatarioResponse;
import com.helisa.docmanager.dto.response.SolicitudDetalleResponse;
import com.helisa.docmanager.dto.response.SolicitudResumenResponse;
import com.helisa.docmanager.model.*;
import com.helisa.docmanager.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private SolicitudDestinatarioRepository destinatarioRepository;
    @Autowired
    private SolicitudAdjuntoRepository adjuntoRepository;
    @Autowired
    private SolicitudHistorialRepository historialRepository;
    @Autowired
    private StorageService storageService;
    @Autowired
    private FlujoAprobacionService flujoService;
    @Autowired
    private EstadoRepository estadoRepository;

    @Value("${storage.max-pdf-bytes:52428800}")
    private long maxPdfBytes;

    @Value("${storage.max-attachment-bytes:10485760}")
    private long maxAttachmentBytes;

    @Value("#{'${storage.allowed-attachment-ext:jpg,png,pdf,docx,xlsx,txt}'.split(',')}")
    private String[] allowedExtensions;

    public SolicitudDetalleResponse crear(CrearSolicitudRequest request) {
        try {
            // Validaciones
            validarPdfPrincipal(request.getPdfPrincipal());
            if (request.getAdjuntos() != null) {
                validarAdjuntos(request.getAdjuntos());
            }

            // Obtener el estado PENDIENTE de la BD
            Estado estadoPendiente = estadoRepository.getEstadoPendiente();

            // Crear solicitud
            Solicitud solicitud = new Solicitud();
            solicitud.setIdSolicitante(request.getIdSolicitante());
            solicitud.setIdTipologia(request.getIdTipologia());
            solicitud.setOrdenFirmaBoolean(request.getOrdenFirma());

            // IMPORTANTE: Establecer el estado desde la BD
            solicitud.setEstado(estadoPendiente);

            // Establecer campos opcionales
            solicitud.setNombreSolicitud("Solicitud de documento - " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            solicitud.setFechaRegistro(LocalDateTime.now());

            // Guardar PDF principal
            String pdfPath = storageService.guardarArchivo(
                    request.getPdfPrincipal(),
                    "pdf_" + System.currentTimeMillis());
            solicitud.setPdfPath(pdfPath);
            solicitud.setPdfOriginalName(request.getPdfPrincipal().getOriginalFilename());
            solicitud.setPdfSizeBytes(request.getPdfPrincipal().getSize());

            // Guardar solicitud
            solicitud = solicitudRepository.save(solicitud);

            log.info("Solicitud guardada con ID: {}, Estado: {}",
                    solicitud.getId(), solicitud.getEstado().getDescripcion());

            // Crear destinatarios
            crearDestinatarios(solicitud, request.getDestinatarios());

            // Guardar adjuntos si existen
            if (request.getAdjuntos() != null) {
                guardarAdjuntos(solicitud, request.getAdjuntos());
            }

            // Crear historial
            crearHistorial(solicitud, request.getIdSolicitante(),
                    SolicitudHistorial.AccionEnum.CREAR,
                    request.getComentarioInicial() != null ?
                            request.getComentarioInicial() : "Solicitud creada");

            log.info("Solicitud creada exitosamente: {}", solicitud.getId());
            return mapearADetalle(solicitud);

        } catch (Exception e) {
            log.error("Error al crear solicitud", e);
            throw new RuntimeException("Error al crear la solicitud: " + e.getMessage(), e);
        }
    }

    public void aprobar(Integer solicitudId, DecisionRequest request) {
        Solicitud solicitud = obtenerSolicitudPendiente(solicitudId);

        if (!flujoService.puedeAprobar(solicitudId, request.getUsuarioId(),
                solicitud.getOrdenFirmaBoolean())) {
            throw new IllegalStateException("Usuario no autorizado para aprobar en este momento");
        }

        SolicitudDestinatario destinatario = destinatarioRepository
                .findBySolicitudIdAndUsuarioId(solicitudId, request.getUsuarioId())
                .orElseThrow(() -> new IllegalStateException("Destinatario no encontrado"));

        destinatario.aprobar(request.getComentario());
        destinatarioRepository.save(destinatario);

        if (flujoService.todosAprobaron(solicitudId)) {
            // Cambiar estado a APROBADO
            Estado estadoAprobado = estadoRepository.getEstadoAprobado();
            solicitud.setEstado(estadoAprobado);
            solicitudRepository.save(solicitud);

            crearHistorial(solicitud, request.getUsuarioId(),
                    SolicitudHistorial.AccionEnum.APROBAR,
                    "Solicitud aprobada por todos los destinatarios");
        }

        log.info("Solicitud {} aprobada por usuario {}", solicitudId, request.getUsuarioId());
    }

    public void rechazar(Integer solicitudId, DecisionRequest request) {
        Solicitud solicitud = obtenerSolicitudPendiente(solicitudId);

        SolicitudDestinatario destinatario = destinatarioRepository
                .findBySolicitudIdAndUsuarioId(solicitudId, request.getUsuarioId())
                .orElseThrow(() -> new IllegalStateException("Usuario no autorizado para rechazar"));

        // Cambiar estado a RECHAZADO
        Estado estadoRechazado = estadoRepository.getEstadoRechazado();
        solicitud.setEstado(estadoRechazado);
        solicitudRepository.save(solicitud);

        destinatario.rechazar(request.getComentario());
        destinatarioRepository.save(destinatario);

        // Eliminar archivos
        eliminarArchivos(solicitud);

        // Crear historial
        crearHistorial(solicitud, request.getUsuarioId(),
                SolicitudHistorial.AccionEnum.RECHAZAR, request.getComentario());

        log.info("Solicitud {} rechazada por usuario {}", solicitudId, request.getUsuarioId());
    }

    public void cancelar(Integer solicitudId, DecisionRequest request) {
        Solicitud solicitud = obtenerSolicitudPendiente(solicitudId);

        // Verificar autorización
        boolean esCreador = solicitud.getIdSolicitante().equals(request.getUsuarioId());
        boolean esDestinatario = destinatarioRepository
                .findBySolicitudIdAndUsuarioId(solicitudId, request.getUsuarioId()).isPresent();

        if (!esCreador && !esDestinatario) {
            throw new IllegalStateException("Usuario no autorizado para cancelar");
        }

        // Cambiar estado a CANCELADA
        Estado estadoCancelado = estadoRepository.getEstadoCancelado();
        solicitud.setEstado(estadoCancelado);
        solicitudRepository.save(solicitud);

        // Eliminar archivos
        eliminarArchivos(solicitud);

        // Crear historial
        SolicitudHistorial historial = SolicitudHistorial.crear(
                solicitud, request.getUsuarioId(),
                SolicitudHistorial.AccionEnum.CANCELAR,
                request.getComentario());
        historialRepository.save(historial);

        log.info("Solicitud {} cancelada por usuario {}", solicitudId, request.getUsuarioId());
    }

    // ================ MÉTODOS AUXILIARES ================

    private Solicitud obtenerSolicitudPendiente(Integer solicitudId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        if (!solicitud.estaPendiente()) {
            throw new IllegalStateException("La solicitud no está en estado PENDIENTE");
        }

        return solicitud;
    }

    @Transactional(readOnly = true)
    public Page<SolicitudResumenResponse> listarPorCreador(Integer creadorId, String estado, Pageable pageable) {
        Page<Solicitud> solicitudes;

        if (estado != null && !estado.trim().isEmpty()) {
            // Buscar el estado en la BD
            Estado estadoEntity = estadoRepository.findByDescripcion(estado.toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Estado no válido: " + estado));

            solicitudes = solicitudRepository.findByIdSolicitanteAndEstado(
                    creadorId, estadoEntity, pageable);
        } else {
            solicitudes = solicitudRepository.findByIdSolicitante(creadorId, pageable);
        }

        return solicitudes.map(this::mapearAResumen);
    }

    // ================ MÉTODOS DE MAPEO ================

    private SolicitudDetalleResponse mapearADetalle(Solicitud solicitud) {
        List<AdjuntoResponse> adjuntos = adjuntoRepository.findBySolicitudId(solicitud.getId())
                .stream()
                .map(adj -> AdjuntoResponse.builder()
                        .id(adj.getId())
                        .originalName(adj.getOriginalName())
                        .sizeBytes(adj.getSizeBytes())
                        .mime(adj.getMime())
                        .build())
                .collect(Collectors.toList());

        List<DestinatarioResponse> destinatarios = destinatarioRepository
                .findBySolicitudId(solicitud.getId())
                .stream()
                .map(dest -> DestinatarioResponse.builder()
                        .usuarioId(dest.getUsuarioId())
                        .ordenIndex(dest.getOrdenIndex())
                        .decision(dest.getDecision().name())
                        .fechaDecision(dest.getFechaDecision())
                        .comentario(dest.getComentario())
                        .build())
                .collect(Collectors.toList());

        Long aprobados = destinatarioRepository.countAprobadosBySolicitudId(solicitud.getId());
        Long total = destinatarioRepository.countTotalBySolicitudId(solicitud.getId());

        return SolicitudDetalleResponse.builder()
                .id(solicitud.getId())
                .estado(solicitud.getEstado().getDescripcion()) // Usar descripción del estado
                .idTipologia(solicitud.getIdTipologia())
                .createdAt(solicitud.getCreatedAt())
                .createdBy(solicitud.getIdSolicitante())
                .ordenFirma(solicitud.getOrdenFirmaBoolean())
                .destinatariosTotal(total.intValue())
                .destinatariosAprobados(aprobados.intValue())
                .pdfOriginalName(solicitud.getPdfOriginalName())
                .pdfSizeBytes(solicitud.getPdfSizeBytes())
                .adjuntos(adjuntos)
                .destinatarios(destinatarios)
                .build();
    }

    private SolicitudResumenResponse mapearAResumen(Solicitud solicitud) {
        Long aprobados = destinatarioRepository.countAprobadosBySolicitudId(solicitud.getId());
        Long total = destinatarioRepository.countTotalBySolicitudId(solicitud.getId());

        return SolicitudResumenResponse.builder()
                .id(solicitud.getId())
                .estado(solicitud.getEstado().getDescripcion()) // Usar descripción del estado
                .idTipologia(solicitud.getIdTipologia())
                .createdAt(solicitud.getCreatedAt())
                .createdBy(solicitud.getIdSolicitante())
                .ordenFirma(solicitud.getOrdenFirmaBoolean())
                .destinatariosTotal(total.intValue())
                .destinatariosAprobados(aprobados.intValue())
                .build();
    }







    private void validarPdfPrincipal(MultipartFile pdf) {
        if (pdf == null || pdf.isEmpty()) {
            throw new IllegalArgumentException("PDF principal es obligatorio");
        }

        String contentType = pdf.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF para el documento principal");
        }

        String fileName = pdf.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("El archivo principal debe tener extensión .pdf");
        }

        if (!storageService.validarTamano(pdf.getSize(), maxPdfBytes)) {
            throw new IllegalArgumentException("PDF excede el tamaño máximo permitido de " +
                    (maxPdfBytes / 1024 / 1024) + "MB");
        }
    }

    private void validarAdjuntos(MultipartFile[] adjuntos) {
        for (MultipartFile adjunto : adjuntos) {
            if (adjunto.isEmpty()) continue;

            String fileName = adjunto.getOriginalFilename();
            if (fileName == null) {
                throw new IllegalArgumentException("Nombre de archivo adjunto no válido");
            }

            if (!storageService.validarExtension(fileName, allowedExtensions)) {
                String extension = "";
                int lastDot = fileName.lastIndexOf('.');
                if (lastDot > 0) {
                    extension = fileName.substring(lastDot + 1);
                }
                throw new IllegalArgumentException("Extensión no permitida para adjuntos: " + extension +
                        ". Extensiones permitidas: " + String.join(", ", allowedExtensions));
            }

            if (!storageService.validarTamano(adjunto.getSize(), maxAttachmentBytes)) {
                throw new IllegalArgumentException("Adjunto '" + fileName + "' excede el tamaño máximo permitido de " +
                        (maxAttachmentBytes / 1024 / 1024) + "MB");
            }
        }
    }



    private void crearDestinatarios(Solicitud solicitud, Integer[] destinatariosIds) {
        List<SolicitudDestinatario> destinatarios = new ArrayList<>();

        for (int i = 0; i < destinatariosIds.length; i++) {
            SolicitudDestinatario destinatario = new SolicitudDestinatario();
            destinatario.setSolicitud(solicitud);
            destinatario.setUsuarioId(destinatariosIds[i]);
            destinatario.setOrdenIndex(i);
            destinatario.setDecision(SolicitudDestinatario.DecisionEnum.PENDIENTE);
            destinatarios.add(destinatario);
        }

        destinatarioRepository.saveAll(destinatarios);
    }

    private void guardarAdjuntos(Solicitud solicitud, MultipartFile[] adjuntos) {
        List<SolicitudAdjunto> adjuntosList = new ArrayList<>();

        for (MultipartFile adjunto : adjuntos) {
            if (adjunto.isEmpty()) continue;

            try {
                String path = storageService.guardarArchivo(adjunto, "adj_" + solicitud.getId());
                String mime = storageService.detectarMimeType(adjunto);

                SolicitudAdjunto solicitudAdjunto = new SolicitudAdjunto();
                solicitudAdjunto.setSolicitud(solicitud);
                solicitudAdjunto.setPath(path);
                solicitudAdjunto.setOriginalName(adjunto.getOriginalFilename());
                solicitudAdjunto.setSizeBytes(adjunto.getSize());
                solicitudAdjunto.setMime(mime);

                adjuntosList.add(solicitudAdjunto);

            } catch (Exception e) {
                log.error("Error al guardar adjunto: {}", adjunto.getOriginalFilename(), e);
                throw new RuntimeException("Error al guardar adjunto: " + adjunto.getOriginalFilename());
            }
        }

        adjuntoRepository.saveAll(adjuntosList);
    }

    private void crearHistorial(Solicitud solicitud, Integer usuarioId,
                                SolicitudHistorial.AccionEnum accion, String comentario) {
        SolicitudHistorial historial = SolicitudHistorial.crear(solicitud, usuarioId, accion, comentario);
        historialRepository.save(historial);
    }

    private void eliminarArchivos(Solicitud solicitud) {
        try {
            if (solicitud.getPdfPath() != null) {
                storageService.borrarArchivo(solicitud.getPdfPath());
            }

            List<SolicitudAdjunto> adjuntos = adjuntoRepository.findBySolicitudId(solicitud.getId());
            for (SolicitudAdjunto adjunto : adjuntos) {
                storageService.borrarArchivo(adjunto.getPath());
            }

            log.info("Archivos eliminados para solicitud: {}", solicitud.getId());

        } catch (Exception e) {
            log.error("Error al eliminar archivos de solicitud: {}", solicitud.getId(), e);
        }
    }


    @Transactional(readOnly = true)
    public SolicitudDetalleResponse obtenerDetalle(Integer solicitudId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        return mapearADetalle(solicitud);
    }





    @Transactional(readOnly = true)
    public Page<SolicitudResumenResponse> listarHistorico(Integer usuarioId, Pageable pageable) {
        Page<Solicitud> solicitudes = solicitudRepository.findHistoricoUsuario(usuarioId, pageable);
        return solicitudes.map(this::mapearAResumen);
    }
    @Transactional(readOnly = true)
    public Page<SolicitudResumenResponse> listarFinalizadas(Integer tipologiaId, String estado, Pageable pageable) {
        Page<Solicitud> solicitudes = solicitudRepository
                .findByIdTipologiaAndEstado_DescripcionIgnoreCase(tipologiaId, estado, pageable);
        return solicitudes.map(this::mapearAResumen);
    }


    @Transactional(readOnly = true)
    public Page<SolicitudResumenResponse> listarParaGestionar(Integer usuarioId, Pageable pageable) {
        Page<Solicitud> solicitudes = solicitudRepository.findPendientesParaGestionar(
                usuarioId, true, pageable);
        return solicitudes.map(this::mapearAResumen);
    }

    @Transactional(readOnly = true)
    public List<AdjuntoResponse> listarAdjuntos(Integer solicitudId) {
        if (!solicitudRepository.existsById(solicitudId)) {
            throw new EntityNotFoundException("Solicitud no encontrada");
        }

        return adjuntoRepository.findBySolicitudId(solicitudId)
                .stream()
                .map(adj -> AdjuntoResponse.builder()
                        .id(adj.getId())
                        .originalName(adj.getOriginalName())
                        .sizeBytes(adj.getSizeBytes())
                        .mime(adj.getMime())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InputStream descargarAdjunto(Integer solicitudId, Long adjuntoId) throws Exception {
        SolicitudAdjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> new EntityNotFoundException("Adjunto no encontrado"));

        if (!adjunto.getSolicitud().getId().equals(solicitudId)) {
            throw new IllegalArgumentException("El adjunto no pertenece a la solicitud especificada");
        }

        return storageService.leerArchivo(adjunto.getPath());
    }

    @Transactional(readOnly = true)
    public InputStream streamPdfPrincipal(Integer solicitudId) throws Exception {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        return storageService.leerArchivo(solicitud.getPdfPath());
    }

    @Transactional(readOnly = true)
    public String obtenerNombrePdf(Integer solicitudId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        return solicitud.getPdfOriginalName();
    }

    @Transactional(readOnly = true)
    public String obtenerNombreAdjunto(Long adjuntoId) {
        SolicitudAdjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> new EntityNotFoundException("Adjunto no encontrado"));

        return adjunto.getOriginalName();
    }





}