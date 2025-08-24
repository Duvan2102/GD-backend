package com.helisa.docmanager.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SolicitudResumenResponse {
    private Long id;
    private String estado;
    private Long tipologiaId;
    private LocalDateTime createdAt;
    private Long createdBy;
    private Boolean ordenFirma;
    private Integer destinatariosTotal;
    private Integer destinatariosAprobados;
}
