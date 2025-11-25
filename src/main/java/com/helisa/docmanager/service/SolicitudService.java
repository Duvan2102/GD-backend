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
import org.springframework.dao.DataIntegrityViolationException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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
    private TipologiaRepository tipologiaRepository;
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
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        if (solicitud.estaAprobado()) {
            throw new IllegalStateException("La solicitud ya fue aprobada anteriormente");
        }
        
        if (solicitud.estaRechazado()) {
            throw new IllegalStateException("La solicitud fue rechazada y no puede ser aprobada");
        }
        
        if (solicitud.estaCancelado()) {
            throw new IllegalStateException("La solicitud fue cancelada y no puede ser aprobada");
        }
        
        boolean esPrimeraRonda = solicitud.estaPendiente();
        boolean esSegundaRonda = solicitud.estaAprobadoProceso();
        
        if (!esPrimeraRonda && !esSegundaRonda) {
            throw new IllegalStateException("La solicitud no está en estado válido para aprobar (debe estar PENDIENTE o APROB_PENDIENTE)");
        }

        if (!flujoService.puedeAprobar(solicitudId, request.getUsuarioId(),
                solicitud.getOrdenFirmaBoolean())) {
            throw new IllegalStateException("Usuario no autorizado para aprobar en este momento");
        }

        Boolean esProcesador = esSegundaRonda;
        SolicitudDestinatario destinatario = destinatarioRepository
                .findBySolicitudIdAndUsuarioIdAndEsProcesador(
                        solicitudId, request.getUsuarioId(), esProcesador)
                .orElseThrow(() -> new IllegalStateException("Destinatario no encontrado"));

        destinatario.aprobar(request.getComentario());
        destinatarioRepository.save(destinatario);

        if (flujoService.todosAprobaron(solicitudId)) {
            if (esPrimeraRonda) {
                boolean requiereProceso = solicitud.getTipologia().getRequiereProceso();
                if (requiereProceso) {
                    Estado estadoAprobPendiente = estadoRepository.getEstadoaAprobPendiente();
                    solicitud.setEstado(estadoAprobPendiente);
                    solicitudRepository.save(solicitud);
                    crearHistorial(solicitud, request.getUsuarioId(), SolicitudHistorial.AccionEnum.APROBAR,
                            "Solicitud aprobada por todos los aprobadores, iniciando proceso");
                } else {
                    Estado estadoAprobado = estadoRepository.getEstadoAprobado();
                    solicitud.setEstado(estadoAprobado);
                    solicitudRepository.save(solicitud);
                    crearHistorial(solicitud, request.getUsuarioId(), SolicitudHistorial.AccionEnum.APROBAR,
                            "Solicitud aprobada por todos los destinatarios");
                }
            } else {
                Estado estadoAprobado = estadoRepository.getEstadoAprobado();
                solicitud.setEstado(estadoAprobado);
                solicitudRepository.save(solicitud);
                crearHistorial(solicitud, request.getUsuarioId(), SolicitudHistorial.AccionEnum.APROBAR,
                        "Solicitud aprobada por todos los procesadores");
            }
        } else {
            if (esPrimeraRonda && Boolean.TRUE.equals(solicitud.getOrdenFirmaBoolean())) {
                notificarSiguienteAprobador(solicitud);
            } else if (esSegundaRonda) {
                notificarSiguienteProcesador(solicitud);
            }
        }

        log.info("Solicitud {} aprobada por usuario {} en {} ronda", 
                solicitudId, request.getUsuarioId(), esSegundaRonda ? "segunda" : "primera");
    }

    public void rechazar(Integer solicitudId, DecisionRequest request) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        if (solicitud.estaAprobado()) {
            throw new IllegalStateException("La solicitud ya fue aprobada y no puede ser rechazada");
        }
        
        if (solicitud.estaRechazado()) {
            throw new IllegalStateException("La solicitud ya fue rechazada anteriormente");
        }
        
        if (solicitud.estaCancelado()) {
            throw new IllegalStateException("La solicitud fue cancelada y no puede ser rechazada");
        }
        
        if (!solicitud.estaPendiente()) {
            throw new IllegalStateException("Solo se puede rechazar en la primera ronda (estado PENDIENTE)");
        }

        SolicitudDestinatario destinatario = destinatarioRepository
                .findBySolicitudIdAndUsuarioIdAndEsProcesador(
                        solicitudId, request.getUsuarioId(), false)
                .orElseThrow(() -> new IllegalStateException("Usuario no autorizado para rechazar"));

        Estado estadoRechazado = estadoRepository.getEstadoRechazado();
        solicitud.setEstado(estadoRechazado);
        solicitudRepository.save(solicitud);

        destinatario.rechazar(request.getComentario());
        destinatarioRepository.save(destinatario);

        crearHistorial(solicitud, request.getUsuarioId(),
                SolicitudHistorial.AccionEnum.RECHAZAR, request.getComentario());

        log.info("Solicitud {} rechazada por usuario {}", solicitudId, request.getUsuarioId());
    }

    public void cancelar(Integer solicitudId, DecisionRequest request) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        // Verificar el estado de la solicitud y proporcionar mensajes descriptivos
        if (solicitud.estaAprobado()) {
            throw new IllegalStateException("La solicitud ya fue aprobada y no puede ser cancelada");
        }
        
        if (solicitud.estaRechazado()) {
            throw new IllegalStateException("La solicitud fue rechazada y no puede ser cancelada");
        }
        
        if (solicitud.estaCancelado()) {
            throw new IllegalStateException("La solicitud ya fue cancelada anteriormente");
        }
        
        if (!solicitud.estaPendiente()) {
            throw new IllegalStateException("La solicitud no está en estado PENDIENTE");
        }

        // Verificar autorización
        boolean esCreador = solicitud.getIdSolicitante().equals(request.getUsuarioId());
        boolean esDestinatario = destinatarioRepository
                .existsBySolicitudIdAndUsuarioId(solicitudId, request.getUsuarioId());

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

    public void agregarProcesadores(Integer solicitudId, ProcesadoresRequest request) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        if (!solicitud.estaAprobadoProceso()) {
            throw new IllegalStateException(
                    "Solo se pueden agregar procesadores a solicitudes en estado APROB_PENDIENTE. " +
                    "Estado actual: " + (solicitud.getEstado() != null ? solicitud.getEstado().getDescripcion() : "DESCONOCIDO"));
        }

        List<Integer> procesadoresIds = Arrays.asList(request.getProcesadores());
        List<Usuario> usuarios = usuarioRepository.findByIdUsuarioIn(procesadoresIds);
        
        if (usuarios.size() != procesadoresIds.size()) {
            List<Integer> usuariosEncontrados = usuarios.stream()
                    .map(Usuario::getIdUsuario)
                    .collect(Collectors.toList());
            List<Integer> usuariosNoEncontrados = procesadoresIds.stream()
                    .filter(id -> !usuariosEncontrados.contains(id))
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("Los siguientes usuarios no existen: " + usuariosNoEncontrados);
        }

        List<SolicitudDestinatario> destinatariosExistentes = destinatarioRepository
                .findBySolicitudId(solicitudId);

        Set<Integer> idsProcesadoresPendientes = destinatariosExistentes.stream()
                .filter(d -> d.getDecision() == SolicitudDestinatario.DecisionEnum.PENDIENTE && d.getEsProcesador())
                .map(SolicitudDestinatario::getUsuarioId)
                .collect(Collectors.toSet());
        
        List<Integer> procesadoresDuplicados = procesadoresIds.stream()
                .filter(idsProcesadoresPendientes::contains)
                .collect(Collectors.toList());
        
        if (!procesadoresDuplicados.isEmpty()) {
            throw new IllegalArgumentException(
                    "Los siguientes usuarios ya están asignados como procesadores pendientes en esta solicitud: " + 
                    procesadoresDuplicados);
        }

        int maxOrdenIndex = destinatariosExistentes.stream()
                .mapToInt(SolicitudDestinatario::getOrdenIndex)
                .max()
                .orElse(-1);

        List<SolicitudDestinatario> nuevosProcesadores = new ArrayList<>();
        int siguienteOrden = maxOrdenIndex + 1;

        for (Integer procesadorId : procesadoresIds) {
            SolicitudDestinatario procesador = new SolicitudDestinatario();
            procesador.setSolicitud(solicitud);
            procesador.setUsuarioId(procesadorId);
            procesador.setOrdenIndex(siguienteOrden++);
            procesador.setDecision(SolicitudDestinatario.DecisionEnum.PENDIENTE);
            procesador.setEsProcesador(true);
            nuevosProcesadores.add(procesador);
        }

        try {
            destinatarioRepository.saveAll(nuevosProcesadores);
        } catch (DataIntegrityViolationException e) {
            log.warn("Violación de integridad al agregar procesadores a solicitud {}: {}", 
                    solicitudId, e.getMessage());
            
            List<SolicitudDestinatario> destinatariosActualizados = destinatarioRepository
                    .findBySolicitudId(solicitudId);
            
            Set<Integer> idsProcesadoresPendientesActualizados = destinatariosActualizados.stream()
                    .filter(d -> d.getDecision() == SolicitudDestinatario.DecisionEnum.PENDIENTE && d.getEsProcesador())
                    .map(SolicitudDestinatario::getUsuarioId)
                    .collect(Collectors.toSet());
            
            List<Integer> procesadoresDuplicadosEnBD = procesadoresIds.stream()
                    .filter(idsProcesadoresPendientesActualizados::contains)
                    .collect(Collectors.toList());
            
            if (!procesadoresDuplicadosEnBD.isEmpty()) {
                throw new IllegalArgumentException(
                        "Los siguientes usuarios ya están asignados como procesadores pendientes en esta solicitud: " + 
                        procesadoresDuplicadosEnBD);
            }
            
            throw new IllegalStateException(
                    "Error al agregar procesadores. Uno o más usuarios ya están asignados a esta solicitud", e);
        }

        String comentario = String.format("Se agregaron %d procesador(es) a la solicitud: %s",
                nuevosProcesadores.size(),
                procesadoresIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")));
        
        crearHistorial(solicitud, solicitud.getIdSolicitante(),
                SolicitudHistorial.AccionEnum.CREAR, comentario);

        enviarNotificacionesNuevosProcesadores(solicitud, nuevosProcesadores);

        log.info("Se agregaron {} procesador(es) a la solicitud {}", nuevosProcesadores.size(), solicitudId);
    }

    // ================ MÉTODOS AUXILIARES ================

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
                        .esProcesador(dest.getEsProcesador())
                        .build())
                .collect(Collectors.toList());

        List<HistorialResponse> historial = historialRepository
                .findBySolicitudIdOrderByFechaAscWithUsuario(solicitud.getId().longValue())
                .stream()
                .map(h -> HistorialResponse.builder()
                        .id(h.getId())
                        .actorUsuarioId(h.getActorUsuarioId())
                        .nombreUsuario(h.getNombreUsuario())
                        .accion(h.getAccion().name())
                        .comentario(h.getComentario())
                        .fecha(h.getFecha())
                        .build())
                .collect(Collectors.toList());

        String descripcionSolicitud = historial.isEmpty() ? null : historial.get(0).getComentario();

        boolean esPrimeraRonda = solicitud.estaPendiente();
        boolean esSegundaRonda = solicitud.estaAprobadoProceso();
        Boolean esProcesador = esSegundaRonda;
        
        Long aprobados = destinatarioRepository.countAprobadosBySolicitudId(solicitud.getId(), esProcesador);
        Long total = destinatarioRepository.countTotalBySolicitudId(solicitud.getId(), esProcesador);

        // Obtener información de la tipología
        String descripcionTipologia = null;
        Boolean requiereProceso = null;
        if (solicitud.getTipologia() != null) {
            descripcionTipologia = solicitud.getTipologia().getDescripcion();
            requiereProceso = solicitud.getTipologia().getRequiereProceso();
        }

        return SolicitudDetalleResponse.builder()
                .id(solicitud.getId())
				.nombreSolicitud(solicitud.getNombreSolicitud())
                .estado(solicitud.getEstado().getDescripcion())
				.descripcionSolicitud(descripcionSolicitud) 
                .idTipologia(solicitud.getIdTipologia())
                .descripcionTipologia(descripcionTipologia)
                .requiereProceso(requiereProceso)
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
                            .nombre(nombre)
                            .ordenIndex(d.getOrdenIndex())
                            .decision(d.getDecision() != null ? d.getDecision().name() : null)
                            .fechaDecision(d.getFechaDecision())
                            .comentario(d.getComentario())
                            .esProcesador(d.getEsProcesador())
                            .build();
                })
                .toList();

        boolean esPrimeraRonda = s.estaPendiente();
        boolean esSegundaRonda = s.estaAprobadoProceso();
        Boolean esProcesador = esSegundaRonda;
        
        Long aprobadosCount = destinatarioRepository.countAprobadosBySolicitudId(s.getId(), esProcesador);
        Long totalCount = destinatarioRepository.countTotalBySolicitudId(s.getId(), esProcesador);
        
        int total = totalCount.intValue();
        int aprobados = aprobadosCount.intValue();

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
            destinatario.setEsProcesador(false);
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

    public List<AdjuntoResponse> agregarAdjuntos(Integer solicitudId, MultipartFile[] archivos, Integer usuarioId) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        if (solicitud.estaRechazado()) {
            throw new IllegalStateException("No se pueden agregar archivos a una solicitud rechazada");
        }

        if (solicitud.estaCancelado()) {
            throw new IllegalStateException("No se pueden agregar archivos a una solicitud cancelada");
        }

        if (archivos == null || archivos.length == 0) {
            throw new IllegalArgumentException("Debe proporcionar al menos un archivo");
        }

        validarAdjuntos(archivos);
        guardarAdjuntos(solicitud, archivos);

        String comentario = String.format("Se agregaron %d archivo(s) adjunto(s) a la solicitud", archivos.length);
        crearHistorial(solicitud, usuarioId, SolicitudHistorial.AccionEnum.CREAR, comentario);

        log.info("Se agregaron {} archivo(s) adjunto(s) a la solicitud {} por usuario {}", 
                archivos.length, solicitudId, usuarioId);

        return listarAdjuntos(solicitudId);
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
            List<SolicitudDestinatario> destinatarios = destinatarioRepository
                    .findPendientesBySolicitudId(solicitud.getId(), false);

            if (destinatarios.isEmpty()) {
                log.warn("No hay aprobadores para notificar en solicitud {}", solicitud.getId());
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

    /**
     * Notifica al siguiente aprobador en la cola cuando una solicitud con orden secuencial
     * es aprobada por un aprobador anterior
     * @param solicitud La solicitud que fue aprobada parcialmente
     */
    private void notificarSiguienteAprobador(Solicitud solicitud) {
        try {
            // Obtener el siguiente aprobador usando FlujoAprobacionService
            SolicitudDestinatario siguienteAprobador = flujoService.obtenerSiguienteAprobador(solicitud.getId());
            
            if (siguienteAprobador == null) {
                log.debug("No hay siguiente aprobador para notificar en solicitud {}", solicitud.getId());
                return;
            }

            // Obtener información del usuario siguiente aprobador
            Usuario usuarioAprobador = usuarioRepository.findById(siguienteAprobador.getUsuarioId()).orElse(null);
            
            if (usuarioAprobador == null) {
                log.warn("Usuario siguiente aprobador no encontrado (ID: {}) para solicitud {}", 
                        siguienteAprobador.getUsuarioId(), solicitud.getId());
                return;
            }

            if (usuarioAprobador.getCorreoEmpresarial() == null || 
                usuarioAprobador.getCorreoEmpresarial().trim().isEmpty()) {
                log.warn("Usuario siguiente aprobador no tiene correo empresarial (ID: {}) para solicitud {}", 
                        siguienteAprobador.getUsuarioId(), solicitud.getId());
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

            // Enviar notificación al siguiente aprobador
            emailService.enviarNotificacionNuevaSolicitud(
                    usuarioAprobador.getCorreoEmpresarial(),
                    solicitud.getId(),
                    solicitud.getNombreSolicitud(),
                    nombreSolicitante,
                    true, // esOrdenSecuencial
                    true  // esSiguienteAprobador
            );

            log.info("Notificación enviada al siguiente aprobador (usuario {}) para solicitud {}",
                    siguienteAprobador.getUsuarioId(), solicitud.getId());

        } catch (Exception e) {
            log.error("Error al notificar siguiente aprobador para solicitud {}: {}", 
                    solicitud.getId(), e.getMessage(), e);
        }
    }

    private void notificarSiguienteProcesador(Solicitud solicitud) {
        try {
            SolicitudDestinatario siguienteProcesador = flujoService.obtenerSiguienteAprobador(solicitud.getId());
            
            if (siguienteProcesador == null) {
                log.debug("No hay siguiente procesador para notificar en solicitud {}", solicitud.getId());
                return;
            }

            Usuario usuarioProcesador = usuarioRepository.findById(siguienteProcesador.getUsuarioId()).orElse(null);
            
            if (usuarioProcesador == null) {
                log.warn("Usuario siguiente procesador no encontrado (ID: {}) para solicitud {}", 
                        siguienteProcesador.getUsuarioId(), solicitud.getId());
                return;
            }

            if (usuarioProcesador.getCorreoEmpresarial() == null || 
                usuarioProcesador.getCorreoEmpresarial().trim().isEmpty()) {
                log.warn("Usuario siguiente procesador no tiene correo empresarial (ID: {}) para solicitud {}", 
                        siguienteProcesador.getUsuarioId(), solicitud.getId());
                return;
            }

            Usuario solicitante = usuarioRepository.findById(solicitud.getIdSolicitante()).orElse(null);
            String nombreSolicitante = solicitante != null ? 
                    Stream.of(solicitante.getNombres(), solicitante.getApellidos())
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(str -> !str.isEmpty())
                            .collect(Collectors.joining(" ")) : "Usuario";

            emailService.enviarNotificacionNuevaSolicitud(
                    usuarioProcesador.getCorreoEmpresarial(),
                    solicitud.getId(),
                    solicitud.getNombreSolicitud(),
                    nombreSolicitante,
                    true,
                    true
            );

            log.info("Notificación enviada al siguiente procesador (usuario {}) para solicitud {}",
                    siguienteProcesador.getUsuarioId(), solicitud.getId());

        } catch (Exception e) {
            log.error("Error al notificar siguiente procesador para solicitud {}: {}", 
                    solicitud.getId(), e.getMessage(), e);
        }
    }

    /**
     * Envía notificaciones por correo a los nuevos procesadores agregados a una solicitud
     * @param solicitud La solicitud a la que se agregaron procesadores
     * @param nuevosProcesadores Lista de los nuevos procesadores agregados
     */
    private void enviarNotificacionesNuevosProcesadores(Solicitud solicitud, 
                                                         List<SolicitudDestinatario> nuevosProcesadores) {
        try {
            if (nuevosProcesadores.isEmpty()) {
                log.warn("No hay nuevos procesadores para notificar en solicitud {}", solicitud.getId());
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

            // Obtener información de los nuevos procesadores
            List<Integer> idsProcesadores = nuevosProcesadores.stream()
                    .map(SolicitudDestinatario::getUsuarioId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Integer, Usuario> usuariosProcesadores = usuarioRepository.findByIdUsuarioIn(idsProcesadores)
                    .stream()
                    .collect(Collectors.toMap(Usuario::getIdUsuario, Function.identity()));

            boolean esOrdenSecuencial = Boolean.TRUE.equals(solicitud.getOrdenFirmaBoolean());

            // Notificar a todos los nuevos procesadores
            for (SolicitudDestinatario procesador : nuevosProcesadores) {
                Usuario usuario = usuariosProcesadores.get(procesador.getUsuarioId());
                if (usuario != null && usuario.getCorreoEmpresarial() != null && 
                    !usuario.getCorreoEmpresarial().trim().isEmpty()) {
                    
                    boolean esSiguienteProcesador = false;
                    List<SolicitudDestinatario> pendientes = destinatarioRepository
                            .findPendientesProcesadoresBySolicitudId(solicitud.getId());
                    if (!pendientes.isEmpty()) {
                        esSiguienteProcesador = pendientes.get(0).getUsuarioId()
                                .equals(procesador.getUsuarioId());
                    }
                    
                    emailService.enviarNotificacionNuevaSolicitud(
                            usuario.getCorreoEmpresarial(),
                            solicitud.getId(),
                            solicitud.getNombreSolicitud(),
                            nombreSolicitante,
                            true,
                            esSiguienteProcesador
                    );
                    
                    log.info("Notificación enviada al nuevo procesador (usuario {}) para solicitud {}",
                            procesador.getUsuarioId(), solicitud.getId());
                }
            }

            log.info("Notificaciones enviadas a {} nuevo(s) procesador(es) para solicitud {}",
                    nuevosProcesadores.size(), solicitud.getId());

        } catch (Exception e) {
            log.error("Error al enviar notificaciones a nuevos procesadores para solicitud {}: {}", 
                    solicitud.getId(), e.getMessage(), e);
            // No lanzar excepción para evitar que falle la adición de procesadores
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
    public Page<SolicitudResumenResponse> listarPorArea(Integer usuarioId, Pageable pageable) {
        // Obtener el usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con id: " + usuarioId));
        
        // Verificar que el usuario tenga cargo
        if (usuario.getCargo() == null || usuario.getCargo().getIdCargo() == null) {
            throw new RuntimeException("El usuario no tiene un cargo asignado");
        }
        
        // Obtener todas las tipologías asociadas al cargo del usuario
        List<Tipologia> tipologias = tipologiaRepository.findByCargoIdCargo(usuario.getCargo().getIdCargo());
        
        // Si no hay tipologías, retornar página vacía
        if (tipologias.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // Extraer los IDs de las tipologías
        List<Integer> tipologiaIds = tipologias.stream()
                .map(Tipologia::getIdTipologia)
                .collect(Collectors.toList());
        
        // Buscar solicitudes que tengan alguna de estas tipologías
        Page<Solicitud> solicitudes = solicitudRepository.findByIdTipologiaIn(tipologiaIds, pageable);
        return solicitudes.map(this::mapearAResumen);
    }


    @Transactional(readOnly = true)
    public Page<SolicitudResumenResponse> listarParaGestionar(Integer usuarioId, Pageable pageable) {
        Page<Solicitud> solicitudes = solicitudRepository.findPendientesParaGestionar(
                usuarioId, true, pageable);
        return solicitudes.map(this::mapearAResumen);
    }

    @Transactional(readOnly = true)
    public Page<SolicitudResumenResponse> listarParaProcesar(Integer usuarioId, Pageable pageable) {
        Page<Solicitud> solicitudes = solicitudRepository.findPendientesParaProcesar(
                usuarioId, pageable);
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
        List<SolicitudDestinatario> destinatarios = destinatarioRepository.findBySolicitudId(solicitudId);

        // Generar PDF de log en memoria
        byte[] pdfLog = generarPdfLog(historial, destinatarios, solicitud);

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

    // Genera PDF con tabla formateada (nombre/accion/fecha/descripcion) y sección de destinatarios
    private byte[] generarPdfLog(List<SolicitudHistorial> historial, List<SolicitudDestinatario> destinatarios, Solicitud solicitud) throws Exception {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);

        PDPageContentStream cs = new PDPageContentStream(doc, page);
        PDType1Font font = PDType1Font.HELVETICA;
        PDType1Font fontBold = PDType1Font.HELVETICA_BOLD;

        float margin = 50f;
        float yStart = page.getMediaBox().getHeight() - margin;
        float leading = 16f;
        float anchoPagina = page.getMediaBox().getWidth() - (margin * 2);
        
        // Anchos de columna (en puntos)
        float anchoNombre = 100f;
        float anchoAccion = 100f;
        float anchoFecha = 95f;
        float anchoDescripcion = anchoPagina - anchoNombre - anchoAccion - anchoFecha;

        yStart -= leading * 2;
        
        cs.beginText();
        cs.setFont(fontBold, 16);
        cs.newLineAtOffset(margin, yStart);
        cs.showText("Log de Solicitud");
        cs.endText();
        yStart -= leading * 1.5f;

        String subtitulo = String.format("ID: %d - Tipología: %s", 
                solicitud.getId(),
                solicitud.getTipologia() != null && solicitud.getTipologia().getDescripcion() != null 
                        ? limpiarTexto(solicitud.getTipologia().getDescripcion()) 
                        : "N/A");
        
        cs.beginText();
        cs.setFont(font, 11);
        cs.newLineAtOffset(margin, yStart);
        cs.showText(subtitulo);
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
            
            // Dividir descripción en múltiples líneas si es necesario
            List<String> lineasDescripcion = dividirTextoEnLineas(comentario, anchoDescripcion, font, 9);
            
            // Salto de página si es necesario (considerando las líneas adicionales de descripción)
            float alturaNecesaria = leading * lineasDescripcion.size();
            if (yStart - alturaNecesaria < margin) {
                cs.endText();
                cs.close();
                page = new PDPage();
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                yStart = page.getMediaBox().getHeight() - margin;
                cs.setFont(font, 9);
            }
            
            // Dibujar las tres primeras columnas en cada línea de descripción
            for (int i = 0; i < lineasDescripcion.size(); i++) {
                cs.beginText();
                cs.newLineAtOffset(margin, yStart);
                
                // Columna 1: Nombre (solo en la primera línea)
                if (i == 0) {
                    String nombreTruncado = truncarTexto(nombre, anchoNombre, font, 9);
                    cs.showText(nombreTruncado);
                }
                
                // Avanzar a la columna de acción
                cs.newLineAtOffset(i == 0 ? anchoNombre : 0, 0);
                
                // Columna 2: Acción (solo en la primera línea)
                if (i == 0) {
                    String accionTruncada = truncarTexto(accion, anchoAccion, font, 9);
                    cs.showText(accionTruncada);
                }
                
                // Avanzar a la columna de fecha
                cs.newLineAtOffset(i == 0 ? anchoAccion : 0, 0);
                
                // Columna 3: Fecha (solo en la primera línea)
                if (i == 0) {
                    String fechaTruncada = truncarTexto(fecha, anchoFecha, font, 9);
                    cs.showText(fechaTruncada);
                }
                
                // Avanzar a la columna de descripción
                cs.newLineAtOffset(i == 0 ? anchoFecha : anchoNombre + anchoAccion + anchoFecha, 0);
                
                // Columna 4: Descripción (puede tener múltiples líneas)
                cs.showText(lineasDescripcion.get(i));
                
                cs.endText();
                yStart -= leading;
            }
        }

        yStart -= leading * 2;

        if (yStart < margin + 100) {
            cs.endText();
            cs.close();
            page = new PDPage();
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            yStart = page.getMediaBox().getHeight() - margin;
        }

        cs.beginText();
        cs.setFont(fontBold, 14);
        cs.newLineAtOffset(margin, yStart);
        cs.showText("Destinatarios y Procesadores");
        cs.endText();
        yStart -= leading * 2;

        List<SolicitudDestinatario> aprobadores = destinatarios.stream()
                .filter(d -> !d.getEsProcesador())
                .sorted(Comparator.comparing(SolicitudDestinatario::getOrdenIndex))
                .collect(Collectors.toList());

        List<SolicitudDestinatario> procesadores = destinatarios.stream()
                .filter(SolicitudDestinatario::getEsProcesador)
                .sorted(Comparator.comparing(SolicitudDestinatario::getOrdenIndex))
                .collect(Collectors.toList());

        if (!aprobadores.isEmpty()) {
            cs.beginText();
            cs.setFont(fontBold, 11);
            cs.newLineAtOffset(margin, yStart);
            cs.showText("Aprobadores:");
            cs.endText();
            yStart -= leading * 1.5f;

            float anchoTipo = 80f;
            float anchoUsuario = 120f;
            float anchoOrden = 50f;
            float anchoEstado = 80f;
            float anchoFechaDest = 95f;
            float anchoComentarioDest = anchoPagina - anchoTipo - anchoUsuario - anchoOrden - anchoEstado - anchoFechaDest;

            cs.beginText();
            cs.setFont(fontBold, 9);
            cs.newLineAtOffset(margin, yStart);
            cs.showText("Tipo");
            cs.newLineAtOffset(anchoTipo, 0);
            cs.showText("Usuario");
            cs.newLineAtOffset(anchoUsuario, 0);
            cs.showText("Orden");
            cs.newLineAtOffset(anchoOrden, 0);
            cs.showText("Estado");
            cs.newLineAtOffset(anchoEstado, 0);
            cs.showText("Fecha");
            cs.newLineAtOffset(anchoFechaDest, 0);
            cs.showText("Comentario");
            cs.endText();
            yStart -= leading;

            cs.setLineWidth(0.5f);
            cs.moveTo(margin, yStart);
            cs.lineTo(margin + anchoPagina, yStart);
            cs.stroke();
            yStart -= leading * 0.5f;

            cs.setFont(font, 8);
            for (SolicitudDestinatario dest : aprobadores) {
                if (yStart < margin + 50) {
                    cs.endText();
                    cs.close();
                    page = new PDPage();
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    yStart = page.getMediaBox().getHeight() - margin;
                    cs.setFont(font, 8);
                }

                Usuario usuario = usuarioRepository.findById(dest.getUsuarioId()).orElse(null);
                String nombreUsuario = usuario != null ?
                        limpiarTexto(Stream.of(usuario.getNombres(), usuario.getApellidos())
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(str -> !str.isEmpty())
                                .collect(Collectors.joining(" "))) : "Usuario " + dest.getUsuarioId();

                String tipo = "Aprobador";
                String estado = dest.getDecision() != null ? dest.getDecision().name() : "PENDIENTE";
                String fechaDest = dest.getFechaDecision() != null ? dest.getFechaDecision().format(fmt) : "";
                String comentarioDest = dest.getComentario() != null ? limpiarTexto(dest.getComentario()) : "";

                List<String> lineasComentario = dividirTextoEnLineas(comentarioDest, anchoComentarioDest, font, 8);
                float alturaNecesaria = leading * lineasComentario.size();

                if (yStart - alturaNecesaria < margin) {
                    cs.endText();
                    cs.close();
                    page = new PDPage();
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    yStart = page.getMediaBox().getHeight() - margin;
                    cs.setFont(font, 8);
                }

                for (int i = 0; i < lineasComentario.size(); i++) {
                    cs.beginText();
                    cs.newLineAtOffset(margin, yStart);

                    if (i == 0) {
                        cs.showText(truncarTexto(tipo, anchoTipo, font, 8));
                        cs.newLineAtOffset(anchoTipo, 0);
                        cs.showText(truncarTexto(nombreUsuario, anchoUsuario, font, 8));
                        cs.newLineAtOffset(anchoUsuario, 0);
                        cs.showText(String.valueOf(dest.getOrdenIndex()));
                        cs.newLineAtOffset(anchoOrden, 0);
                        cs.showText(truncarTexto(estado, anchoEstado, font, 8));
                        cs.newLineAtOffset(anchoEstado, 0);
                        cs.showText(truncarTexto(fechaDest, anchoFechaDest, font, 8));
                        cs.newLineAtOffset(anchoFechaDest, 0);
                    } else {
                        cs.newLineAtOffset(anchoTipo + anchoUsuario + anchoOrden + anchoEstado + anchoFechaDest, 0);
                    }

                    cs.showText(lineasComentario.get(i));
                    cs.endText();
                    yStart -= leading;
                }
            }
        }

        if (!procesadores.isEmpty()) {
            yStart -= leading;
            if (yStart < margin + 100) {
                cs.endText();
                cs.close();
                page = new PDPage();
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                yStart = page.getMediaBox().getHeight() - margin;
            }

            cs.beginText();
            cs.setFont(fontBold, 11);
            cs.newLineAtOffset(margin, yStart);
            cs.showText("Procesadores:");
            cs.endText();
            yStart -= leading * 1.5f;

            float anchoTipo = 80f;
            float anchoUsuario = 120f;
            float anchoOrden = 50f;
            float anchoEstado = 80f;
            float anchoFechaDest = 95f;
            float anchoComentarioDest = anchoPagina - anchoTipo - anchoUsuario - anchoOrden - anchoEstado - anchoFechaDest;

            cs.beginText();
            cs.setFont(fontBold, 9);
            cs.newLineAtOffset(margin, yStart);
            cs.showText("Tipo");
            cs.newLineAtOffset(anchoTipo, 0);
            cs.showText("Usuario");
            cs.newLineAtOffset(anchoUsuario, 0);
            cs.showText("Orden");
            cs.newLineAtOffset(anchoOrden, 0);
            cs.showText("Estado");
            cs.newLineAtOffset(anchoEstado, 0);
            cs.showText("Fecha");
            cs.newLineAtOffset(anchoFechaDest, 0);
            cs.showText("Comentario");
            cs.endText();
            yStart -= leading;

            cs.setLineWidth(0.5f);
            cs.moveTo(margin, yStart);
            cs.lineTo(margin + anchoPagina, yStart);
            cs.stroke();
            yStart -= leading * 0.5f;

            cs.setFont(font, 8);
            for (SolicitudDestinatario dest : procesadores) {
                if (yStart < margin + 50) {
                    cs.endText();
                    cs.close();
                    page = new PDPage();
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    yStart = page.getMediaBox().getHeight() - margin;
                    cs.setFont(font, 8);
                }

                Usuario usuario = usuarioRepository.findById(dest.getUsuarioId()).orElse(null);
                String nombreUsuario = usuario != null ?
                        limpiarTexto(Stream.of(usuario.getNombres(), usuario.getApellidos())
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(str -> !str.isEmpty())
                                .collect(Collectors.joining(" "))) : "Usuario " + dest.getUsuarioId();

                String tipo = "Procesador";
                String estado = dest.getDecision() != null ? dest.getDecision().name() : "PENDIENTE";
                String fechaDest = dest.getFechaDecision() != null ? dest.getFechaDecision().format(fmt) : "";
                String comentarioDest = dest.getComentario() != null ? limpiarTexto(dest.getComentario()) : "";

                List<String> lineasComentario = dividirTextoEnLineas(comentarioDest, anchoComentarioDest, font, 8);
                float alturaNecesaria = leading * lineasComentario.size();

                if (yStart - alturaNecesaria < margin) {
                    cs.endText();
                    cs.close();
                    page = new PDPage();
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    yStart = page.getMediaBox().getHeight() - margin;
                    cs.setFont(font, 8);
                }

                for (int i = 0; i < lineasComentario.size(); i++) {
                    cs.beginText();
                    cs.newLineAtOffset(margin, yStart);

                    if (i == 0) {
                        cs.showText(truncarTexto(tipo, anchoTipo, font, 8));
                        cs.newLineAtOffset(anchoTipo, 0);
                        cs.showText(truncarTexto(nombreUsuario, anchoUsuario, font, 8));
                        cs.newLineAtOffset(anchoUsuario, 0);
                        cs.showText(String.valueOf(dest.getOrdenIndex()));
                        cs.newLineAtOffset(anchoOrden, 0);
                        cs.showText(truncarTexto(estado, anchoEstado, font, 8));
                        cs.newLineAtOffset(anchoEstado, 0);
                        cs.showText(truncarTexto(fechaDest, anchoFechaDest, font, 8));
                        cs.newLineAtOffset(anchoFechaDest, 0);
                    } else {
                        cs.newLineAtOffset(anchoTipo + anchoUsuario + anchoOrden + anchoEstado + anchoFechaDest, 0);
                    }

                    cs.showText(lineasComentario.get(i));
                    cs.endText();
                    yStart -= leading;
                }
            }
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

    /**
     * Divide un texto en múltiples líneas según el ancho máximo disponible
     * Cada línea cabrá dentro del ancho especificado
     */
    private List<String> dividirTextoEnLineas(String texto, float anchoMaximo, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize) {
        List<String> lineas = new ArrayList<>();
        
        if (texto == null || texto.isEmpty()) {
            lineas.add("");
            return lineas;
        }
        
        try {
            String[] palabras = texto.split("\\s+");
            StringBuilder lineaActual = new StringBuilder();
            
            for (String palabra : palabras) {
                String textoPrueba = lineaActual.length() == 0 ? palabra : lineaActual.toString() + " " + palabra;
                float anchoTexto = font.getStringWidth(textoPrueba) / 1000 * fontSize;
                
                if (anchoTexto <= anchoMaximo) {
                    if (lineaActual.length() > 0) {
                        lineaActual.append(" ");
                    }
                    lineaActual.append(palabra);
                } else {
                    // La palabra no cabe, empezar nueva línea
                    if (lineaActual.length() > 0) {
                        lineas.add(lineaActual.toString());
                        lineaActual = new StringBuilder();
                    }
                    // Si la palabra sola es demasiado larga, truncarla
                    if (font.getStringWidth(palabra) / 1000 * fontSize > anchoMaximo) {
                        palabra = truncarTexto(palabra, anchoMaximo, font, fontSize).replace("...", "");
                    }
                    lineaActual.append(palabra);
                }
            }
            
            // Agregar la última línea
            if (lineaActual.length() > 0) {
                lineas.add(lineaActual.toString());
            } else if (lineas.isEmpty()) {
                lineas.add("");
            }
            
        } catch (Exception e) {
            // Fallback: dividir por caracteres
            int maxChars = (int) (anchoMaximo / (fontSize * 0.6f));
            while (texto.length() > maxChars) {
                lineas.add(texto.substring(0, maxChars));
                texto = texto.substring(maxChars);
            }
            if (!texto.isEmpty() || lineas.isEmpty()) {
                lineas.add(texto);
            }
        }
        
        return lineas;
    }




}