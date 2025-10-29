package com.helisa.docmanager.service;

import com.helisa.docmanager.dto.response.AdjuntoResponse;
import com.helisa.docmanager.dto.response.DestinatarioResponse;
import com.helisa.docmanager.dto.response.HistorialResponse;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmailService emailService;

    @Value("${storage.max-pdf-bytes:52428800}")
    private long maxPdfBytes;

    @Value("${storage.max-attachment-bytes:10485760}")
    private long maxAttachmentBytes;

    @Value("#{'${storage.allowed-attachment-ext:jpg,png,pdf,docx,xlsx,txt}'.split(',')}")
    private String[] allowedExtensions;

    public SolicitudDetalleResponse crear(CrearSolicitudRequest request) {
        try {
            validarPdfPrincipal(request.getPdfPrincipal());
            if (request.getAdjuntos() != null) {
                validarAdjuntos(request.getAdjuntos());
            }

            Estado estadoPendiente = estadoRepository.getEstadoPendiente();

            Solicitud solicitud = new Solicitud();
            solicitud.setIdSolicitante(request.getIdSolicitante());
            solicitud.setIdTipologia(request.getIdTipologia());
            solicitud.setOrdenFirmaBoolean(request.getOrdenFirma());
            solicitud.setPrioridad(request.getPrioridad());
            solicitud.setEnviarRecordatorio(request.getEnviarRecordatorio());

            solicitud.setEstado(estadoPendiente);

            solicitud.setNombreSolicitud(request.getNombreSolicitud());
            solicitud.setFechaRegistro(LocalDateTime.now());

            String pdfPath = storageService.guardarArchivo(
                    request.getPdfPrincipal(),
                    "pdf_" + System.currentTimeMillis());
            solicitud.setPdfPath(pdfPath);
            solicitud.setPdfOriginalName(request.getPdfPrincipal().getOriginalFilename());
            solicitud.setPdfSizeBytes(request.getPdfPrincipal().getSize());

            solicitud = solicitudRepository.save(solicitud);

            log.info("Solicitud guardada con ID: {}, Estado: {}",
                    solicitud.getId(), solicitud.getEstado().getDescripcion());

            crearDestinatarios(solicitud, request.getDestinatarios());

            if (request.getAdjuntos() != null) {
                guardarAdjuntos(solicitud, request.getAdjuntos());
            }

            crearHistorial(solicitud, request.getIdSolicitante(),
                    SolicitudHistorial.AccionEnum.CREAR,
                    request.getComentarioInicial() != null ?
                            request.getComentarioInicial() : "Solicitud creada");

            // Enviar notificaciones a los aprobadores
            enviarNotificacionesNuevaSolicitud(solicitud);

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

        // NOTA: Los archivos NO se eliminan cuando se rechaza una solicitud
        // para mantener un historial completo de documentos

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

        // NOTA: Los archivos NO se eliminan cuando se cancela una solicitud
        // para mantener un historial completo de documentos

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

        List<HistorialResponse> historial = historialRepository
                .findBySolicitudIdOrderByFechaAscWithUsuario(solicitud.getId().longValue())
                .stream()
                .map(h -> HistorialResponse.builder()
                        .id(h.getId())
                        .actorUsuarioId(h.getActorUsuarioId())
                        .nombreUsuario(h.getNombreUsuario()) // Ahora incluye el nombre
                        .accion(h.getAccion().name())
                        .comentario(h.getComentario())
                        .fecha(h.getFecha())
                        .build())
                .collect(Collectors.toList());

        // Obtener el comentario inicial del primer registro del historial
        String descripcionSolicitud = historial.isEmpty() ? null : historial.get(0).getComentario();

        Long aprobados = destinatarioRepository.countAprobadosBySolicitudId(solicitud.getId());
        Long total = destinatarioRepository.countTotalBySolicitudId(solicitud.getId());

        return SolicitudDetalleResponse.builder()
                .id(solicitud.getId())
				.nombreSolicitud(solicitud.getNombreSolicitud())
                .estado(solicitud.getEstado().getDescripcion())
				.descripcionSolicitud(descripcionSolicitud) 
                .idTipologia(solicitud.getIdTipologia())
                .createdAt(solicitud.getCreatedAt())
                .createdBy(solicitud.getIdSolicitante())
                .ordenFirma(solicitud.getOrdenFirmaBoolean())
                .prioridad(solicitud.getPrioridad())
                .enviarRecordatorio(solicitud.getEnviarRecordatorio())
                .destinatariosTotal(total.intValue())
                .destinatariosAprobados(aprobados.intValue())
                .pdfOriginalName(solicitud.getPdfOriginalName())
                .pdfSizeBytes(solicitud.getPdfSizeBytes())
                .adjuntos(adjuntos)
                .destinatarios(destinatarios)
                .historial(historial)
                .build();
    }

    private SolicitudResumenResponse mapearAResumen(Solicitud s) {
        // ---- Preload de usuarios (solicitante + destinatarios) en 1 query ----
        Set<Integer> ids = new HashSet<>();
        if (s.getIdSolicitante() != null) ids.add(s.getIdSolicitante());
        if (s.getDestinatariosDetalle() != null) {
            for (SolicitudDestinatario d : s.getDestinatariosDetalle()) {
                if (d.getUsuarioId() != null) ids.add(d.getUsuarioId());
            }
        }

        Map<Integer, Usuario> usuarios = ids.isEmpty()
                ? Collections.emptyMap()
                : usuarioRepository.findByIdUsuarioIn(ids).stream()
                .collect(Collectors.toMap(Usuario::getIdUsuario, Function.identity()));

        // ---- Solicitante (nombre/cargo si lo necesitas) ----
        Usuario solicitante = usuarios.get(s.getIdSolicitante());
        String solicitanteNombre = (solicitante == null) ? null :
                Stream.of(solicitante.getNombres(), solicitante.getApellidos())
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(str -> !str.isEmpty())
                        .collect(Collectors.joining(" "));

        String cargoSolicitante = (solicitante == null) ? null :
                Stream.of(solicitante.getCargo().getDescripcion())
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(str -> !str.isEmpty())
                        .collect(Collectors.joining(" "));

        List<SolicitudDestinatario> det =
                (s.getDestinatariosDetalle() != null) ? s.getDestinatariosDetalle() : List.of();

        List<DestinatarioResponse> destinatarios = det.stream()
                .map(d -> {
                    Usuario u = usuarios.get(d.getUsuarioId());
                    String nombre = (u == null) ? null :
                            Stream.of(u.getNombres(), u.getApellidos())
                                    .filter(Objects::nonNull)
                                    .map(String::trim)
                                    .filter(str -> !str.isEmpty())
                                    .collect(Collectors.joining(" "));
                    return DestinatarioResponse.builder()
                            .usuarioId(d.getUsuarioId())
                            .nombre(nombre) // <-- nuevo en tu DTO
                            .ordenIndex(d.getOrdenIndex())
                            .decision(d.getDecision() != null ? d.getDecision().name() : null)
                            .fechaDecision(d.getFechaDecision())
                            .comentario(d.getComentario())
                            .build();
                })
                .toList();

        int total = destinatarios.size();
        int aprobados = (int) destinatarios.stream()
                .map(DestinatarioResponse::getDecision)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .filter("APROBADO"::equals)
                .count();

        return SolicitudResumenResponse.builder()
                .id(s.getId())
                .estado(s.getEstado() != null ? s.getEstado().getDescripcion() : null)
                .idTipologia(s.getIdTipologia())
                .createdAt(s.getCreatedAt())
                .createdBy(s.getIdSolicitante())
                .solicitanteCargo(cargoSolicitante)
                .solicitanteName(solicitanteNombre)
                .ordenFirma(Boolean.TRUE.equals(s.getOrdenFirmaBoolean()))
                .prioridad(s.getPrioridad())
                .enviarRecordatorio(s.getEnviarRecordatorio())
                .destinatarios(destinatarios)
                .destinatariosTotal(total)
                .destinatariosAprobados(aprobados)
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

    /**
     * Envía notificaciones por correo a los aprobadores cuando se crea una nueva solicitud
     * @param solicitud La solicitud recién creada
     */
    private void enviarNotificacionesNuevaSolicitud(Solicitud solicitud) {
        try {
            // Obtener todos los destinatarios de la solicitud
            List<SolicitudDestinatario> destinatarios = destinatarioRepository
                    .findBySolicitudId(solicitud.getId());

            if (destinatarios.isEmpty()) {
                log.warn("No hay destinatarios para notificar en solicitud {}", solicitud.getId());
                return;
            }

            // Obtener información del solicitante
            Usuario solicitante = usuarioRepository.findById(solicitud.getIdSolicitante()).orElse(null);
            String nombreSolicitante = solicitante != null ? 
                    Stream.of(solicitante.getNombres(), solicitante.getApellidos())
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(str -> !str.isEmpty())
                            .collect(Collectors.joining(" ")) : "Usuario";

            // Obtener información de todos los destinatarios
            List<Integer> idsDestinatarios = destinatarios.stream()
                    .map(SolicitudDestinatario::getUsuarioId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Integer, Usuario> usuariosDestinatarios = usuarioRepository.findByIdUsuarioIn(idsDestinatarios)
                    .stream()
                    .collect(Collectors.toMap(Usuario::getIdUsuario, Function.identity()));

            boolean esOrdenSecuencial = Boolean.TRUE.equals(solicitud.getOrdenFirmaBoolean());

            if (esOrdenSecuencial) {
                // Para orden secuencial, solo notificar al primer aprobador
                SolicitudDestinatario primerAprobador = destinatarios.stream()
                        .min(Comparator.comparing(SolicitudDestinatario::getOrdenIndex))
                        .orElse(null);

                if (primerAprobador != null) {
                    Usuario usuario = usuariosDestinatarios.get(primerAprobador.getUsuarioId());
                    if (usuario != null && usuario.getCorreoEmpresarial() != null && 
                        !usuario.getCorreoEmpresarial().trim().isEmpty()) {
                        
                        emailService.enviarNotificacionNuevaSolicitud(
                                usuario.getCorreoEmpresarial(),
                                solicitud.getId(),
                                solicitud.getNombreSolicitud(),
                                nombreSolicitante,
                                true, // esOrdenSecuencial
                                true  // esSiguienteAprobador
                        );
                        
                        log.info("Notificación enviada al primer aprobador (usuario {}) para solicitud {}",
                                primerAprobador.getUsuarioId(), solicitud.getId());
                    }
                }
            } else {
                // Para aprobación simultánea, notificar a todos los destinatarios
                for (SolicitudDestinatario destinatario : destinatarios) {
                    Usuario usuario = usuariosDestinatarios.get(destinatario.getUsuarioId());
                    if (usuario != null && usuario.getCorreoEmpresarial() != null && 
                        !usuario.getCorreoEmpresarial().trim().isEmpty()) {
                        
                        emailService.enviarNotificacionNuevaSolicitud(
                                usuario.getCorreoEmpresarial(),
                                solicitud.getId(),
                                solicitud.getNombreSolicitud(),
                                nombreSolicitante,
                                false, // esOrdenSecuencial
                                false  // esSiguienteAprobador
                        );
                    }
                }
                
                log.info("Notificaciones enviadas a {} destinatarios para solicitud {} (aprobación simultánea)",
                        destinatarios.size(), solicitud.getId());
            }

        } catch (Exception e) {
            log.error("Error al enviar notificaciones de nueva solicitud {}: {}", 
                    solicitud.getId(), e.getMessage(), e);
            // No lanzar excepción para evitar que falle la creación de la solicitud
        }
    }

    @Transactional
    public void registrarDescarga(Integer solicitudId, Integer usuarioId, String tipoDescarga) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        String nombreUsuario = usuario.getUsuario(); 

        SolicitudHistorial.AccionEnum accion;
        String comentario;

        switch (tipoDescarga.toUpperCase()) {
            case "DESCARGAR_ARCHIVO_PRINCIPAL":
                accion = SolicitudHistorial.AccionEnum.DESCARGAR_PRINCIPAL;
                comentario = "Usuario " + nombreUsuario + " descargó el archivo principal de la solicitud";
                break;
            case "DESCARGAR_ADJUNTOS":
                accion = SolicitudHistorial.AccionEnum.DESCARGAR_ADJUNTOS;
                comentario = "Usuario " + nombreUsuario + " descargó adjuntos de la solicitud";
                break;
            case "DESCARGAR_COMPLETA":
                accion = SolicitudHistorial.AccionEnum.DESCARGAR_COMPLETA;
                comentario = "Usuario " + nombreUsuario + " descargó todos los archivos de la solicitud (ZIP completo)";
                break;
            default:
                accion = SolicitudHistorial.AccionEnum.DESCARGAR;
                comentario = "Usuario " + nombreUsuario + " descargó archivos de la solicitud";
                break;
        }

        crearHistorial(solicitud, usuarioId, accion, comentario);
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


    // Genera un ZIP con: PDF principal, adjuntos y un PDF con tabla de log (nombre/acción/fecha)
    @Transactional(readOnly = true)
    public InputStream descargarTodoComoZip(Integer solicitudId) throws Exception {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        List<SolicitudAdjunto> adjuntos = adjuntoRepository.findBySolicitudId(solicitudId);
        List<SolicitudHistorial> historial = historialRepository.findBySolicitudIdOrderByFechaAscWithUsuario(solicitudId.longValue());

        // Generar PDF de log en memoria
        byte[] pdfLog = generarPdfLog(historial);

        // Crear ZIP en memoria
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            
            // PDF principal - verificar existencia antes de agregar
            if (solicitud.getPdfPath() != null && archivoExiste(solicitud.getPdfPath())) {
                try (InputStream pdfPrincipal = storageService.leerArchivo(solicitud.getPdfPath())) {
                    agregarEntradaZip(zos, "principal/" + solicitud.getPdfOriginalName(), pdfPrincipal);
                }
            } else {
                log.warn("PDF principal no encontrado para solicitud {}: {}", solicitudId, solicitud.getPdfPath());
                // Agregar archivo de notificación en lugar del PDF faltante
                String mensaje = "PDF principal no disponible: " + solicitud.getPdfPath();
                agregarEntradaZip(zos, "principal/ARCHIVO_NO_ENCONTRADO.txt", 
                    new java.io.ByteArrayInputStream(mensaje.getBytes()));
            }

            // Adjuntos - verificar existencia antes de agregar
            for (SolicitudAdjunto adj : adjuntos) {
                if (archivoExiste(adj.getPath())) {
                    try (InputStream is = storageService.leerArchivo(adj.getPath())) {
                        agregarEntradaZip(zos, "adjuntos/" + adj.getOriginalName(), is);
                    }
                } else {
                    log.warn("Adjunto no encontrado para solicitud {}: {}", solicitudId, adj.getPath());
                    // Agregar archivo de notificación en lugar del adjunto faltante
                    String mensaje = "Adjunto no disponible: " + adj.getOriginalName() + " (" + adj.getPath() + ")";
                    agregarEntradaZip(zos, "adjuntos/" + adj.getOriginalName() + ".NO_ENCONTRADO.txt", 
                        new java.io.ByteArrayInputStream(mensaje.getBytes()));
                }
            }

            // PDF de log
            agregarEntradaZip(zos, "log/log-solicitud-" + solicitudId + ".pdf", new java.io.ByteArrayInputStream(pdfLog));
        }

        return new java.io.ByteArrayInputStream(baos.toByteArray());
    }

    private void agregarEntradaZip(java.util.zip.ZipOutputStream zos, String nombreEntrada, InputStream contenido) throws Exception {
        zos.putNextEntry(new java.util.zip.ZipEntry(nombreEntrada));
        contenido.transferTo(zos);
        zos.closeEntry();
        contenido.close();
    }

    private boolean archivoExiste(String path) {
        try {
            java.nio.file.Path rutaCompleta = java.nio.file.Paths.get(storageService.getBasePath(), path);
            return java.nio.file.Files.exists(rutaCompleta);
        } catch (Exception e) {
            log.warn("Error verificando existencia del archivo: {}", path, e);
            return false;
        }
    }

    // Genera PDF con tabla formateada (nombre/accion/fecha/descripcion)
    private byte[] generarPdfLog(List<SolicitudHistorial> historial) throws Exception {
        org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
        doc.addPage(page);

        org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);
        org.apache.pdfbox.pdmodel.font.PDType1Font font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
        org.apache.pdfbox.pdmodel.font.PDType1Font fontBold = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;

        float margin = 50f;
        float yStart = page.getMediaBox().getHeight() - margin;
        float leading = 16f;
        float anchoPagina = page.getMediaBox().getWidth() - (margin * 2);
        
        // Anchos de columna (en puntos)
        float anchoNombre = 120f;
        float anchoAccion = 100f;
        float anchoFecha = 150f;
        float anchoDescripcion = anchoPagina - anchoNombre - anchoAccion - anchoFecha;

        yStart -= leading * 2;
        
        cs.beginText();
        cs.setFont(fontBold, 16);
        cs.newLineAtOffset(margin, yStart);
        cs.showText("Log de Solicitud");
        cs.endText();
        yStart -= leading * 2;

        // Encabezados de la tabla
        cs.beginText();
        cs.setFont(fontBold, 10);
        cs.newLineAtOffset(margin, yStart);
        cs.showText("Nombre");
        cs.newLineAtOffset(anchoNombre, 0);
        cs.showText("Acción");
        cs.newLineAtOffset(anchoAccion, 0);
        cs.showText("Fecha");
        cs.newLineAtOffset(anchoFecha, 0);
        cs.showText("Descripción");
        cs.endText();
        yStart -= leading;

        // Línea separadora debajo de encabezados
        cs.setLineWidth(1f);
        cs.moveTo(margin, yStart);
        cs.lineTo(margin + anchoPagina, yStart);
        cs.stroke();
        yStart -= leading * 0.5f;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Datos de la tabla
        cs.setFont(font, 9);
        for (SolicitudHistorial h : historial) {
            String nombre = h.getNombreUsuario() != null ? limpiarTexto(h.getNombreUsuario()) : "Sistema";
            String accion = h.getAccion() != null ? h.getAccion().name() : "";
            String comentario = h.getComentario() != null ? limpiarTexto(h.getComentario()) : "";
            String fecha = h.getFecha() != null ? h.getFecha().format(fmt) : "";
            
            // Salto de página si es necesario
            if (yStart < margin) {
                cs.endText();
                cs.close();
                page = new org.apache.pdfbox.pdmodel.PDPage();
                doc.addPage(page);
                cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);
                yStart = page.getMediaBox().getHeight() - margin;
                cs.setFont(font, 9);
            }
            
            // Dibujar fila
            cs.beginText();
            cs.newLineAtOffset(margin, yStart);
            
            // Columna 1: Nombre
            String nombreTruncado = truncarTexto(nombre, anchoNombre, font, 9);
            cs.showText(nombreTruncado);
            
            // Columna 2: Acción
            cs.newLineAtOffset(anchoNombre, 0);
            String accionTruncada = truncarTexto(accion, anchoAccion, font, 9);
            cs.showText(accionTruncada);
            
            // Columna 3: Fecha
            cs.newLineAtOffset(anchoAccion, 0);
            String fechaTruncada = truncarTexto(fecha, anchoFecha, font, 9);
            cs.showText(fechaTruncada);
            
            // Columna 4: Descripción (permite truncar con "...")
            cs.newLineAtOffset(anchoFecha, 0);
            String descripcionTruncada = truncarTexto(comentario, anchoDescripcion, font, 9);
            cs.showText(descripcionTruncada);
            
            cs.endText();
            yStart -= leading;
        }

        cs.close();

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        doc.save(baos);
        doc.close();
        return baos.toByteArray();
    }

    /**
     * Limpia el texto eliminando caracteres de control y caracteres especiales
     * que no son compatibles con la codificación WinAnsiEncoding de PDFBox
     */
    private String limpiarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        // Reemplazar saltos de línea y retornos de carro por espacios
        texto = texto.replace('\r', ' ');
        texto = texto.replace('\n', ' ');
        texto = texto.replace('\t', ' ');
        
        // Eliminar cualquier otro carácter de control (caracteres fuera del rango ASCII 32-126)
        texto = texto.replaceAll("[\\x00-\\x1F]", " ");
        
        // Normalizar espacios múltiples a uno solo
        texto = texto.replaceAll("\\s+", " ");
        
        return texto.trim();
    }

    /**
     * Trunca el texto si excede el ancho máximo permitido en puntos
     * Usa "..." al final si el texto fue truncado
     */
    private String truncarTexto(String texto, float anchoMaximo, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        
        try {
            float anchoTexto = font.getStringWidth(texto) / 1000 * fontSize;
            
            if (anchoTexto <= anchoMaximo) {
                return texto;
            }
            
            // Buscar el punto de corte usando búsqueda binaria
            int longitud = texto.length();
            String textoTruncado = texto;
            
            while (font.getStringWidth(textoTruncado + "...") / 1000 * fontSize > anchoMaximo && longitud > 0) {
                longitud--;
                textoTruncado = texto.substring(0, longitud);
            }
            
            return textoTruncado + "...";
        } catch (Exception e) {
            // Si hay error calculando el ancho, simplemente truncar por caracteres
            int maxChars = (int) (anchoMaximo / (fontSize * 0.6f)); // Aproximación
            if (texto.length() > maxChars) {
                return texto.substring(0, maxChars) + "...";
            }
            return texto;
        }
    }




}