package com.helisa.docmanager.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcesadoresRequest {

    @NotNull(message = "Lista de procesadores es obligatoria")
    @NotEmpty(message = "Debe incluir al menos un procesador")
    private Integer[] procesadores;
}

