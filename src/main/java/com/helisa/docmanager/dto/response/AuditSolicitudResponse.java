package com.helisa.docmanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSolicitudResponse {
    private Integer id;
    private String nombreSolicitud;
    private String estado;
    private String tipologia;
    private String departamento;
    private String solicitanteNombre;
    private String solicitanteCargo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime createdAt;
    private Integer createdBy;
    private Boolean ordenFirma;
    private Boolean prioridad;
    private Integer enviarRecordatorio;
    private Integer destinatariosTotal;
    private Integer destinatariosAprobados;
    private String pdfOriginalName;
    private Long pdfSizeBytes;
    private List<DestinatarioResponse> destinatarios;
    private List<HistorialResponse> historial;
}
