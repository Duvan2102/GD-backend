package com.helisa.docmanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SolicitudDetalleResponse {
    private Integer id;
    private String estado;
    private Integer idTipologia;
    private LocalDateTime createdAt;
    private Integer createdBy;
	private String nombreSolicitud;
	private String descripcionSolicitud;
    private Boolean ordenFirma;
    private Integer destinatariosTotal;
    private Integer destinatariosAprobados;

    private String pdfOriginalName;
    private Long pdfSizeBytes;

    private List<AdjuntoResponse> adjuntos;
    private List<DestinatarioResponse> destinatarios;
    private List<HistorialResponse> historial;
}
