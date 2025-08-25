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
public class SolicitudResumenResponse {
    private Integer id;
    private String estado;
    private Integer idTipologia;
    private String solicitanteName;
    private String solicitanteCargo;
    private LocalDateTime createdAt;
    private Integer createdBy;
    private Boolean ordenFirma;
    private Integer destinatariosTotal;
    private Integer destinatariosAprobados;
    private List<DestinatarioResponse> destinatarios;
}
