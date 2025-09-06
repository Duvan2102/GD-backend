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
public class HistorialResponse {
    private Long id;
    private Integer actorUsuarioId;
    private String accion;
    private String comentario;
    private LocalDateTime fecha;
}
