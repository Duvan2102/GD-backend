package com.helisa.docmanager.service;

import com.helisa.docmanager.dto.AuditSolicitudResponse;
import com.helisa.docmanager.dto.ExcelAuditRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelAuditService {
    
    @Autowired
    private AuditService auditService;
    
    public byte[] generateExcelAuditReport(ExcelAuditRequest request) throws IOException {
        // Obtener las solicitudes con historial
        List<AuditSolicitudResponse> solicitudes = auditService.getSolicitudesForExcel(request.getSelectedIds());
        
        // Crear el workbook y la hoja
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Auditoría de Solicitudes");
        
        // Crear estilos
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);
        
        // Crear encabezados
        createHeaders(sheet, headerStyle);
        
        // Llenar datos
        int rowNum = 1;
        for (AuditSolicitudResponse solicitud : solicitudes) {
            rowNum = createSolicitudRow(sheet, solicitud, dataStyle, dateStyle, rowNum);
        }
        
        // Ajustar ancho de columnas
        adjustColumnWidths(sheet);
        
        // Convertir a byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return outputStream.toByteArray();
    }
    
    private void createHeaders(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        
        String[] headers = {
            "ID Solicitud", "Nombre Solicitud", "Tipología", "Estado", "Solicitante",
            "Departamento", "Área", "Cargo", "Fecha Registro", "Detalles Adicionales",
            "Prioridad", "Enviar Recordatorio", "Documento Principal", "Documentos Adicionales",
            "Destinatarios", "Orden Firma", "Historial - Usuario", "Historial - Descripción", "Historial - Fecha"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }
    
    private int createSolicitudRow(Sheet sheet, AuditSolicitudResponse solicitud, CellStyle dataStyle, CellStyle dateStyle, int startRow) {
        int currentRow = startRow;
        
        // Si no hay historial, crear una sola fila
        if (solicitud.getHistorial() == null || solicitud.getHistorial().isEmpty()) {
            Row row = sheet.createRow(currentRow);
            fillSolicitudData(row, solicitud, dataStyle, dateStyle, null);
            return currentRow + 1;
        }
        
        // Si hay historial, crear una fila por cada entrada del historial
        for (AuditSolicitudResponse.HistorialComentario historial : solicitud.getHistorial()) {
            Row row = sheet.createRow(currentRow);
            fillSolicitudData(row, solicitud, dataStyle, dateStyle, historial);
            currentRow++;
        }
        
        return currentRow;
    }
    
    private void fillSolicitudData(Row row, AuditSolicitudResponse solicitud, CellStyle dataStyle, CellStyle dateStyle, AuditSolicitudResponse.HistorialComentario historial) {
        int cellNum = 0;
        
        // Datos básicos de la solicitud
        createCell(row, cellNum++, solicitud.getIdSolicitud() != null ? solicitud.getIdSolicitud().toString() : "", dataStyle);
        createCell(row, cellNum++, solicitud.getNombreSolicitud(), dataStyle);
        createCell(row, cellNum++, solicitud.getTipologia(), dataStyle);
        createCell(row, cellNum++, solicitud.getEstado(), dataStyle);
        createCell(row, cellNum++, solicitud.getSolicitante(), dataStyle);
        createCell(row, cellNum++, solicitud.getDepartamento(), dataStyle);
        createCell(row, cellNum++, solicitud.getArea(), dataStyle);
        createCell(row, cellNum++, solicitud.getCargo(), dataStyle);
        
        // Fecha de registro
        if (solicitud.getFechaRegistro() != null) {
            Cell dateCell = row.createCell(cellNum++);
            dateCell.setCellValue(solicitud.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dateCell.setCellStyle(dateStyle);
        } else {
            createCell(row, cellNum++, "", dataStyle);
        }
        
        createCell(row, cellNum++, solicitud.getDetallesAdicionales(), dataStyle);
        createCell(row, cellNum++, solicitud.getPrioridad(), dataStyle);
        createCell(row, cellNum++, solicitud.getEnviarRecordatorio() != null ? (solicitud.getEnviarRecordatorio() ? "Sí" : "No") : "", dataStyle);
        createCell(row, cellNum++, solicitud.getDocumentoPrincipal(), dataStyle);
        createCell(row, cellNum++, solicitud.getDocumentosAdicionales(), dataStyle);
        createCell(row, cellNum++, solicitud.getDestinatarios(), dataStyle);
        createCell(row, cellNum++, solicitud.getOrdenFirma(), dataStyle);
        
        // Datos del historial
        if (historial != null) {
            createCell(row, cellNum++, historial.getUsuario(), dataStyle);
            createCell(row, cellNum++, historial.getDescripcion(), dataStyle);
            if (historial.getFechaRegistro() != null) {
                Cell histDateCell = row.createCell(cellNum++);
                histDateCell.setCellValue(historial.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                histDateCell.setCellStyle(dateStyle);
            } else {
                createCell(row, cellNum++, "", dataStyle);
            }
        } else {
            createCell(row, cellNum++, "", dataStyle);
            createCell(row, cellNum++, "", dataStyle);
            createCell(row, cellNum++, "", dataStyle);
        }
    }
    
    private void createCell(Row row, int cellNum, String value, CellStyle style) {
        Cell cell = row.createCell(cellNum);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
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
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }
    
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
        return style;
    }
    
    private void adjustColumnWidths(Sheet sheet) {
        // Ajustar ancho de columnas basado en el contenido
        for (int i = 0; i < 19; i++) {
            sheet.autoSizeColumn(i);
            // Establecer un ancho mínimo y máximo
            int currentWidth = sheet.getColumnWidth(i);
            int newWidth = Math.max(2000, Math.min(8000, currentWidth));
            sheet.setColumnWidth(i, newWidth);
        }
    }
}