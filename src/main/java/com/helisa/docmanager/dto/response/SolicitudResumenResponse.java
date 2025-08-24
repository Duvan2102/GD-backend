package com.helisa.docmanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudResumenResponse {
    private Integer id;
    private String estado;
    private Integer idTipologia;
    private LocalDateTime createdAt;
    private Integer createdBy;
    private Boolean ordenFirma;
    private Integer destinatariosTotal;
    private Integer destinatariosAprobados;
}
