package com.helisa.docmanager.service;

import com.helisa.docmanager.dto.response.AuditSolicitudResponse;
import com.helisa.docmanager.dto.response.DestinatarioResponse;
import com.helisa.docmanager.dto.response.HistorialResponse;
import com.helisa.docmanager.model.*;
import com.helisa.docmanager.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
@Slf4j
public class AuditService {

    @Autowired
    private SolicitudRepository solicitudRepository;
    
    @Autowired
    private SolicitudDestinatarioRepository destinatarioRepository;
    
    @Autowired
    private SolicitudHistorialRepository historialRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private EstadoRepository estadoRepository;
    
    @Autowired
    private TipologiaRepository tipologiaRepository;
    
    @Autowired
    private DepartamentoRepository departamentoRepository;

    /**
     * Busca solicitudes con filtros de auditoría
     */
    @Transactional(readOnly = true)
    public Page<AuditSolicitudResponse> buscarSolicitudesAuditoria(AuditSearchRequest request, Pageable pageable) {
        log.info("Buscando solicitudes de auditoría con filtros: {}", request);
        
        // Validar que al menos un filtro esté presente
        validateFilters(request.getFilters());
        
        // Construir query dinámico basado en los filtros
        Page<Solicitud> solicitudes = buildAuditQuery(request.getFilters(), pageable);
        
        return solicitudes.map(this::mapearAAuditResponse);
    }

    /**
     * Genera Excel de auditoría para las solicitudes seleccionadas
     */
    @Transactional(readOnly = true)
    public byte[] generarExcelAuditoria(AuditExcelRequest request) {
        log.info("Generando Excel de auditoría para {} solicitudes", request.getSelectedIds().size());
        
        // Convertir IDs de String a Integer
        List<Integer> solicitudIds = request.getSelectedIds().stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        
        // Obtener solicitudes con toda la información necesaria
        List<Solicitud> solicitudes = solicitudRepository.findAllById(solicitudIds);
        
        if (solicitudes.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron solicitudes con los IDs proporcionados");
        }
        
        return generarExcel(solicitudes);
    }

    private void validateFilters(AuditSearchRequest.Filters filters) {
        boolean hasAnyFilter = false;
        
        if (filters.getFechas() != null && 
            (filters.getFechas().getFechaDesde() != null || filters.getFechas().getFechaHasta() != null)) {
            hasAnyFilter = true;
        }
        
        if (filters.getSolicitante() != null && !filters.getSolicitante().trim().isEmpty()) {
            hasAnyFilter = true;
        }
        
        if (filters.getEstado() != null && !filters.getEstado().trim().isEmpty()) {
            hasAnyFilter = true;
        }
        
        if (filters.getTipologia() != null && !filters.getTipologia().trim().isEmpty()) {
            hasAnyFilter = true;
        }
        
        if (filters.getDepartamento() != null && !filters.getDepartamento().trim().isEmpty()) {
            hasAnyFilter = true;
        }
        
        if (!hasAnyFilter) {
            throw new IllegalArgumentException("Debe proporcionar al menos un filtro de búsqueda");
        }
    }

    private Page<Solicitud> buildAuditQuery(AuditSearchRequest.Filters filters, Pageable pageable) {
        // Esta es una implementación simplificada. En un caso real, usarías Criteria API o QueryDSL
        // para construir queries dinámicas más eficientes
        
        if (filters.getFechas() != null && 
            (filters.getFechas().getFechaDesde() != null || filters.getFechas().getFechaHasta() != null)) {
            
            LocalDateTime fechaDesde = filters.getFechas().getFechaDesde() != null ? 
                filters.getFechas().getFechaDesde().atStartOfDay() : null;
            LocalDateTime fechaHasta = filters.getFechas().getFechaHasta() != null ? 
                filters.getFechas().getFechaHasta().atTime(23, 59, 59) : null;
            
            if (fechaDesde != null && fechaHasta != null) {
                return solicitudRepository.findByCreatedAtBetween(fechaDesde, fechaHasta, pageable);
            } else if (fechaDesde != null) {
                return solicitudRepository.findByCreatedAtAfter(fechaDesde, pageable);
            } else {
                return solicitudRepository.findByCreatedAtBefore(fechaHasta, pageable);
            }
        }
        
        // Si no hay filtro de fechas, buscar por otros criterios
        if (filters.getEstado() != null && !filters.getEstado().trim().isEmpty()) {
            Estado estado = estadoRepository.findByDescripcion(filters.getEstado().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Estado no válido: " + filters.getEstado()));
            return solicitudRepository.findByEstado(estado, pageable);
        }
        
        // Por defecto, retornar todas las solicitudes (esto se puede mejorar con más filtros)
        return solicitudRepository.findAll(pageable);
    }

    private AuditSolicitudResponse mapearAAuditResponse(Solicitud solicitud) {
        // Obtener información del solicitante
        Usuario solicitante = usuarioRepository.findById(solicitud.getIdSolicitante()).orElse(null);
        String solicitanteNombre = obtenerNombreCompleto(solicitante);
        String solicitanteCargo = obtenerCargo(solicitante);
        String departamento = obtenerDepartamento(solicitante);
        
        // Obtener tipología
        String tipologia = obtenerTipologia(solicitud.getIdTipologia());
        
        // Obtener destinatarios
        List<DestinatarioResponse> destinatarios = destinatarioRepository
                .findBySolicitudId(solicitud.getId())
                .stream()
                .map(dest -> {
                    Usuario usuario = usuarioRepository.findById(dest.getUsuarioId()).orElse(null);
                    String nombre = obtenerNombreCompleto(usuario);
                    return DestinatarioResponse.builder()
                            .usuarioId(dest.getUsuarioId())
                            .nombre(nombre)
                            .ordenIndex(dest.getOrdenIndex())
                            .decision(dest.getDecision() != null ? dest.getDecision().name() : null)
                            .fechaDecision(dest.getFechaDecision())
                            .comentario(dest.getComentario())
                            .build();
                })
                .collect(Collectors.toList());
        
        // Obtener historial
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
        
        // Contar aprobaciones
        Long aprobados = destinatarioRepository.countAprobadosBySolicitudId(solicitud.getId());
        Long total = destinatarioRepository.countTotalBySolicitudId(solicitud.getId());
        
        return AuditSolicitudResponse.builder()
                .id(solicitud.getId())
                .nombreSolicitud(solicitud.getNombreSolicitud())
                .estado(solicitud.getEstado().getDescripcion())
                .tipologia(tipologia)
                .departamento(departamento)
                .solicitanteNombre(solicitanteNombre)
                .solicitanteCargo(solicitanteCargo)
                .fechaRegistro(solicitud.getFechaRegistro())
                .createdAt(solicitud.getCreatedAt())
                .createdBy(solicitud.getIdSolicitante())
                .ordenFirma(solicitud.getOrdenFirmaBoolean())
                .prioridad(solicitud.getPrioridad())
                .enviarRecordatorio(solicitud.getEnviarRecordatorio())
                .destinatariosTotal(total.intValue())
                .destinatariosAprobados(aprobados.intValue())
                .pdfOriginalName(solicitud.getPdfOriginalName())
                .pdfSizeBytes(solicitud.getPdfSizeBytes())
                .destinatarios(destinatarios)
                .historial(historial)
                .build();
    }

    private String obtenerNombreCompleto(Usuario usuario) {
        if (usuario == null) return null;
        return Stream.of(usuario.getNombres(), usuario.getApellidos())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private String obtenerCargo(Usuario usuario) {
        if (usuario == null || usuario.getCargo() == null) return null;
        return usuario.getCargo().getDescripcion();
    }

    private String obtenerDepartamento(Usuario usuario) {
        if (usuario == null || usuario.getCargo() == null || 
            usuario.getCargo().getArea() == null || 
            usuario.getCargo().getArea().getDepartamento() == null) {
            return null;
        }
        return usuario.getCargo().getArea().getDepartamento().getDescripcion();
    }

    private String obtenerTipologia(Integer idTipologia) {
        if (idTipologia == null) return null;
        return tipologiaRepository.findById(idTipologia)
                .map(Tipologia::getDescripcion)
                .orElse(null);
    }

    private byte[] generarExcel(List<Solicitud> solicitudes) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Auditoría de Solicitudes");
            
            // Crear estilos
            CellStyle headerStyle = crearEstiloEncabezado(workbook);
            CellStyle dataStyle = crearEstiloDatos(workbook);
            CellStyle dateStyle = crearEstiloFecha(workbook);
            
            int rowNum = 0;
            
            // Encabezados principales
            Row headerRow = sheet.createRow(rowNum++);
            crearEncabezados(headerRow, headerStyle);
            
            // Datos de solicitudes
            for (Solicitud solicitud : solicitudes) {
                AuditSolicitudResponse auditResponse = mapearAAuditResponse(solicitud);
                
                // Fila principal de la solicitud
                Row solicitudRow = sheet.createRow(rowNum++);
                crearFilaSolicitud(solicitudRow, auditResponse, dataStyle, dateStyle);
                
                // Filas de historial
                for (HistorialResponse historial : auditResponse.getHistorial()) {
                    Row historialRow = sheet.createRow(rowNum++);
                    crearFilaHistorial(historialRow, historial, dataStyle, dateStyle);
                }
                
                // Fila vacía para separar solicitudes
                rowNum++;
            }
            
            // Ajustar ancho de columnas
            ajustarAnchoColumnas(sheet);
            
            // Convertir a bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("Error generando Excel de auditoría", e);
            throw new RuntimeException("Error generando Excel de auditoría", e);
        }
    }

    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloDatos(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloFecha(Workbook workbook) {
        CellStyle style = crearEstiloDatos(workbook);
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy hh:mm:ss"));
        return style;
    }

    private void crearEncabezados(Row row, CellStyle style) {
        String[] encabezados = {
            "ID Solicitud", "Nombre Solicitud", "Estado", "Tipología", "Departamento",
            "Solicitante", "Cargo Solicitante", "Fecha Registro", "Orden Firma",
            "Prioridad", "Total Destinatarios", "Aprobados", "PDF Original",
            "Tamaño PDF (bytes)", "Tipo Registro", "Usuario", "Acción", "Comentario", "Fecha"
        };
        
        for (int i = 0; i < encabezados.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(encabezados[i]);
            cell.setCellStyle(style);
        }
    }

    private void crearFilaSolicitud(Row row, AuditSolicitudResponse audit, CellStyle dataStyle, CellStyle dateStyle) {
        int col = 0;
        
        // Datos principales de la solicitud
        crearCelda(row, col++, audit.getId().toString(), dataStyle);
        crearCelda(row, col++, audit.getNombreSolicitud(), dataStyle);
        crearCelda(row, col++, audit.getEstado(), dataStyle);
        crearCelda(row, col++, audit.getTipologia(), dataStyle);
        crearCelda(row, col++, audit.getDepartamento(), dataStyle);
        crearCelda(row, col++, audit.getSolicitanteNombre(), dataStyle);
        crearCelda(row, col++, audit.getSolicitanteCargo(), dataStyle);
        crearCeldaFecha(row, col++, audit.getFechaRegistro(), dateStyle);
        crearCelda(row, col++, audit.getOrdenFirma() ? "Sí" : "No", dataStyle);
        crearCelda(row, col++, audit.getPrioridad() ? "Sí" : "No", dataStyle);
        crearCelda(row, col++, audit.getDestinatariosTotal().toString(), dataStyle);
        crearCelda(row, col++, audit.getDestinatariosAprobados().toString(), dataStyle);
        crearCelda(row, col++, audit.getPdfOriginalName(), dataStyle);
        crearCelda(row, col++, audit.getPdfSizeBytes().toString(), dataStyle);
        
        // Campos para historial (se llenarán en las filas de historial)
        crearCelda(row, col++, "SOLICITUD", dataStyle);
        crearCelda(row, col++, "", dataStyle);
        crearCelda(row, col++, "", dataStyle);
        crearCelda(row, col++, "", dataStyle);
        crearCelda(row, col++, "", dataStyle);
    }

    private void crearFilaHistorial(Row row, HistorialResponse historial, CellStyle dataStyle, CellStyle dateStyle) {
        int col = 0;
        
        // Campos vacíos para alineación con la fila de solicitud
        for (int i = 0; i < 14; i++) {
            crearCelda(row, col++, "", dataStyle);
        }
        
        // Datos del historial
        crearCelda(row, col++, "HISTORIAL", dataStyle);
        crearCelda(row, col++, historial.getNombreUsuario(), dataStyle);
        crearCelda(row, col++, historial.getAccion(), dataStyle);
        crearCelda(row, col++, historial.getComentario(), dataStyle);
        crearCeldaFecha(row, col++, historial.getFecha(), dateStyle);
    }

    private void crearCelda(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void crearCeldaFecha(Row row, int col, LocalDateTime date, CellStyle style) {
        Cell cell = row.createCell(col);
        if (date != null) {
            cell.setCellValue(java.sql.Timestamp.valueOf(date));
        }
        cell.setCellStyle(style);
    }

    private void ajustarAnchoColumnas(Sheet sheet) {
        // Ajustar ancho de columnas automáticamente
        for (int i = 0; i < 19; i++) {
            sheet.autoSizeColumn(i);
            // Limitar el ancho máximo
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth > 15000) {
                sheet.setColumnWidth(i, 15000);
            }
        }
    }
}
