package com.helisa.docmanager.service;

import com.helisa.docmanager.dto.response.AdjuntoResponse;
import com.helisa.docmanager.dto.response.DestinatarioResponse;
import com.helisa.docmanager.dto.response.SolicitudDetalleResponse;
import com.helisa.docmanager.dto.response.SolicitudResumenResponse;
import com.helisa.docmanager.model.*;
import com.helisa.docmanager.repository.SolicitudAdjuntoRepository;
import com.helisa.docmanager.repository.SolicitudDestinatarioRepository;
import com.helisa.docmanager.repository.SolicitudHistorialRepository;
import com.helisa.docmanager.repository.SolicitudRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired private SolicitudDestinatarioRepository destinatarioRepository;
    @Autowired private SolicitudAdjuntoRepository adjuntoRepository;
    @Autowired private SolicitudHistorialRepository historialRepository;
    @Autowired private StorageService storageService;
    @Autowired private FlujoAprobacionService flujoService;

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

            // Crear solicitud
            Solicitud solicitud = new Solicitud();
            solicitud.setIdSolicitante(request.getIdSolicitante());
            solicitud.setTipologiaId(request.getTipologiaId());
            solicitud.setOrdenFirma(request.getOrdenFirma());
            solicitud.setEstado(Solicitud.EstadoSolicitud.PENDIENTE);

            // Guardar PDF principal
            String pdfPath = storageService.guardarArchivo(
                    request.getPdfPrincipal(), "pdf_" + System.currentTimeMillis());
            solicitud.setPdfPath(pdfPath);
            solicitud.setPdfOriginalName(request.getPdfPrincipal().getOriginalFilename());
            solicitud.setPdfSizeBytes(request.getPdfPrincipal().getSize());

            solicitud = solicitudRepository.save(solicitud);

            // Crear destinatarios
            crearDestinatarios(solicitud, request.getDestinatarios());

            // Guardar adjuntos si existen
            if (request.getAdjuntos() != null) {
                guardarAdjuntos(solicitud, request.getAdjuntos());
            }

            // Crear historial
            crearHistorial(solicitud, request.getIdSolicitante(),
                    SolicitudHistorial.AccionEnum.CREAR,
                    request.getComentarioInicial());

            log.info("Solicitud creada: {}", solicitud.getId());
            return mapearADetalle(solicitud);

        } catch (Exception e) {
            log.error("Error al crear solicitud", e);
            throw new RuntimeException("Error al crear la solicitud: " + e.getMessage());
        }
    }

    public void aprobar(Long solicitudId, DecisionRequest request) {
        Solicitud solicitud = obtenerSolicitudPendiente(solicitudId);

        if (!flujoService.puedeAprobar(solicitudId, request.getUsuarioId(),
                solicitud.getOrdenFirma())) {
            throw new IllegalStateException("Usuario no autorizado para aprobar en este momento");
        }

        // Actualizar decisión del destinatario
        SolicitudDestinatario destinatario = destinatarioRepository
                .findBySolicitudIdAndUsuarioId(solicitudId, request.getUsuarioId())
                .orElseThrow(() -> new IllegalStateException("Destinatario no encontrado"));

        destinatario.setDecision(SolicitudDestinatario.DecisionEnum.APROBADO);
        destinatario.setComentario(request.getComentario());
        destinatario.setFechaDecision(LocalDateTime.now());
        destinatarioRepository.save(destinatario);

        // Verificar si todos aprobaron
        if (flujoService.todosAprobaron(solicitudId)) {
            solicitud.setEstado(Solicitud.EstadoSolicitud.APROBADO);
            solicitudRepository.save(solicitud);

            crearHistorial(solicitud, request.getUsuarioId(),
                    SolicitudHistorial.AccionEnum.APROBAR,
                    "Solicitud aprobada por todos los destinatarios");
        }

        log.info("Solicitud {} aprobada por usuario {}", solicitudId, request.getUsuarioId());
    }

    public void rechazar(Long solicitudId, DecisionRequest request) {
        Solicitud solicitud = obtenerSolicitudPendiente(solicitudId);

        // Verificar que el usuario sea destinatario
        SolicitudDestinatario destinatario = destinatarioRepository
                .findBySolicitudIdAndUsuarioId(solicitudId, request.getUsuarioId())
                .orElseThrow(() -> new IllegalStateException("Usuario no autorizado para rechazar"));

        // Actualizar estado
        solicitud.setEstado(Solicitud.EstadoSolicitud.RECHAZADO);
        solicitudRepository.save(solicitud);

        destinatario.setDecision(SolicitudDestinatario.DecisionEnum.RECHAZADO);
        destinatario.setComentario(request.getComentario());
        destinatario.setFechaDecision(LocalDateTime.now());
        destinatarioRepository.save(destinatario);

        // Eliminar archivos
        eliminarArchivos(solicitud);

        // Crear historial
        crearHistorial(solicitud, request.getUsuarioId(),
                SolicitudHistorial.AccionEnum.RECHAZAR, request.getComentario());

        log.info("Solicitud {} rechazada por usuario {}", solicitudId, request.getUsuarioId());
    }

    public void cancelar(Long solicitudId, DecisionRequest request) {
        Solicitud solicitud = obtenerSolicitudPendiente(solicitudId);

        // Verificar autorización (creador o destinatario)
        boolean esCreador = solicitud.getIdSolicitante().equals(request.getUsuarioId());
        boolean esDestinatario = destinatarioRepository
                .findBySolicitudIdAndUsuarioId(solicitudId, request.getUsuarioId()).isPresent();

        if (!esCreador && !esDestinatario) {
            throw new IllegalStateException("Usuario no autorizado para cancelar");
        }

        // Actualizar estado
        solicitud.setEstado(Solicitud.EstadoSolicitud.CANCELADA);
        solicitudRepository.save(solicitud);

        // Eliminar archivos
        eliminarArchivos(solicitud);

        // Crear historial
        crearHistorial(solicitud, request.getUsuarioId(),
                SolicitudHistorial.AccionEnum.CANCELAR, request.getComentario());

        log.info("Solicitud {} cancelada por usuario {}", solicitudId, request.getUsuarioId());
    }

    // Métodos auxiliares privados...
    private void validarPdfPrincipal(MultipartFile pdf) {
        if (pdf == null || pdf.isEmpty()) {
            throw new IllegalArgumentException("PDF principal es obligatorio");
        }
        if (!pdf.getContentType().equals("application/pdf")) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF");
        }
        if (!storageService.validarTamano(pdf.getSize(), maxPdfBytes)) {
            throw new IllegalArgumentException("PDF excede el tamaño máximo permitido");
        }
    }

    private Solicitud obtenerSolicitudPendiente(Long solicitudId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        if (solicitud.getEstado() != Solicitud.EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("La solicitud no está en estado PENDIENTE");
        }

        return solicitud;
    }
    private void validarAdjuntos(MultipartFile[] adjuntos) {
        for (MultipartFile adjunto : adjuntos) {
            if (adjunto.isEmpty()) continue;

            if (!storageService.validarExtension(adjunto.getOriginalFilename(), allowedExtensions)) {
                throw new IllegalArgumentException("Extensión no permitida: " +
                        FilenameUtils.getExtension(adjunto.getOriginalFilename()));
            }

            if (!storageService.validarTamano(adjunto.getSize(), maxAttachmentBytes)) {
                throw new IllegalArgumentException("Adjunto excede el tamaño máximo permitido: " +
                        adjunto.getOriginalFilename());
            }
        }
    }

    private void crearDestinatarios(Solicitud solicitud, Long[] destinatariosIds) {
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

    private void crearHistorial(Solicitud solicitud, Long usuarioId,
                                SolicitudHistorial.AccionEnum accion, String comentario) {
        SolicitudHistorial historial = new SolicitudHistorial();
        historial.setSolicitud(solicitud);
        historial.setActorUsuarioId(usuarioId);
        historial.setAccion(accion);
        historial.setComentario(comentario);
        historial.setFecha(LocalDateTime.now());

        historialRepository.save(historial);
    }

    private void eliminarArchivos(Solicitud solicitud) {
        try {
            // Eliminar PDF principal
            if (solicitud.getPdfPath() != null) {
                storageService.borrarArchivo(solicitud.getPdfPath());
            }

            // Eliminar adjuntos
            List<SolicitudAdjunto> adjuntos = adjuntoRepository.findBySolicitudId(solicitud.getId());
            for (SolicitudAdjunto adjunto : adjuntos) {
                storageService.borrarArchivo(adjunto.getPath());
            }

            log.info("Archivos eliminados para solicitud: {}", solicitud.getId());

        } catch (Exception e) {
            log.error("Error al eliminar archivos de solicitud: {}", solicitud.getId(), e);
            // No revertir transacción por error de eliminación de archivos
        }
    }

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
                .estado(solicitud.getEstado().name())
                .tipologiaId(solicitud.getTipologiaId())
                .createdAt(solicitud.getCreatedAt())
                .createdBy(solicitud.getIdSolicitante())
                .ordenFirma(solicitud.getOrdenFirma())
                .destinatariosTotal(total.intValue())
                .destinatariosAprobados(aprobados.intValue())
                .pdfOriginalName(solicitud.getPdfOriginalName())
                .pdfSizeBytes(solicitud.getPdfSizeBytes())
                .adjuntos(adjuntos)
                .destinatarios(destinatarios)
                .build();
    }

    @Transactional(readOnly = true)
    public SolicitudDetalleResponse obtenerDetalle(Long solicitudId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        return mapearADetalle(solicitud);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudResumenResponse> listarPorCreador(Long creadorId, String estado, Pageable pageable) {
        Page<Solicitud> solicitudes;

        if (estado != null && !estado.trim().isEmpty()) {
            Solicitud.EstadoSolicitud estadoEnum = Solicitud.EstadoSolicitud.valueOf(estado.toUpperCase());
            solicitudes = solicitudRepository.findByIdSolicitanteAndEstado(creadorId, estadoEnum, pageable);
        } else {
            solicitudes = solicitudRepository.findByIdSolicitante(creadorId, pageable);
        }

        return solicitudes.map(this::mapearAResumen);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudResumenResponse> listarParaGestionar(Long usuarioId, Pageable pageable) {
        // Buscar solicitudes donde el usuario puede gestionar
        Page<Solicitud> solicitudes = solicitudRepository.findPendientesParaGestionar(
                usuarioId, true, pageable); // Considerar orden secuencial

        return solicitudes.map(this::mapearAResumen);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudResumenResponse> listarHistorico(Long usuarioId, Pageable pageable) {
        Page<Solicitud> solicitudes = solicitudRepository.findHistoricoUsuario(usuarioId, pageable);
        return solicitudes.map(this::mapearAResumen);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudResumenResponse> listarFinalizadas(Long tipologiaId, String estado, Pageable pageable) {
        Solicitud.EstadoSolicitud estadoEnum = Solicitud.EstadoSolicitud.valueOf(estado.toUpperCase());
        Page<Solicitud> solicitudes = solicitudRepository.findByTipologiaIdAndEstado(tipologiaId, estadoEnum, pageable);
        return solicitudes.map(this::mapearAResumen);
    }

    @Transactional(readOnly = true)
    public List<AdjuntoResponse> listarAdjuntos(Long solicitudId) {
        // Verificar que la solicitud existe
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
    public InputStream descargarAdjunto(Long solicitudId, Long adjuntoId) throws Exception {
        SolicitudAdjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> new EntityNotFoundException("Adjunto no encontrado"));

        if (!adjunto.getSolicitud().getId().equals(solicitudId)) {
            throw new IllegalArgumentException("El adjunto no pertenece a la solicitud especificada");
        }

        return storageService.leerArchivo(adjunto.getPath());
    }

    @Transactional(readOnly = true)
    public InputStream streamPdfPrincipal(Long solicitudId) throws Exception {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        return storageService.leerArchivo(solicitud.getPdfPath());
    }

    @Transactional(readOnly = true)
    public String obtenerNombrePdf(Long solicitudId) {
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

    private SolicitudResumenResponse mapearAResumen(Solicitud solicitud) {
        Long aprobados = destinatarioRepository.countAprobadosBySolicitudId(solicitud.getId());
        Long total = destinatarioRepository.countTotalBySolicitudId(solicitud.getId());

        return SolicitudResumenResponse.builder()
                .id(solicitud.getId())
                .estado(solicitud.getEstado().name())
                .tipologiaId(solicitud.getTipologiaId())
                .createdAt(solicitud.getCreatedAt())
                .createdBy(solicitud.getIdSolicitante())
                .ordenFirma(solicitud.getOrdenFirma())
                .destinatariosTotal(total.intValue())
                .destinatariosAprobados(aprobados.intValue())
                .build();
    }

}
