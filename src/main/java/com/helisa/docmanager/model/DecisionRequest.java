package com.helisa.docmanager.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRequest {

    @NotNull(message = "ID del usuario es obligatorio")
    @Positive(message = "ID del usuario debe ser positivo")
    private Integer usuarioId;

    @NotBlank(message = "El comentario es obligatorio para tomar una decisión")
    private String comentario;
}


