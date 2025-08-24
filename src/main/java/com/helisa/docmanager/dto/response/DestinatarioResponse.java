package com.helisa.docmanager.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DestinatarioResponse {
    private Integer usuarioId;
    private Integer ordenIndex;
    private String decision;
    private LocalDateTime fechaDecision;
    private String comentario;
}
