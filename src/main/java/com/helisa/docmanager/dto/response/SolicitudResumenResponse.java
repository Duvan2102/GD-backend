package com.helisa.docmanager.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SolicitudResumenResponse {
    private Integer id;
    private String estado;
    private Integer tipologiaId;
    private LocalDateTime createdAt;
    private Integer createdBy;
    private Boolean ordenFirma;
    private Integer destinatariosTotal;
    private Integer destinatariosAprobados;
}
