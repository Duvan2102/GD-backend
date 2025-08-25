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
public class DestinatarioResponse {
    private Integer usuarioId;
    private Integer ordenIndex;
    private String nombre;
    private String decision;
    private LocalDateTime fechaDecision;
    private String comentario;
}
