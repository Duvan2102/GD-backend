package com.helisa.docmanager.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarPasswordRequest {
    @NotNull
    private String nuevaPassword;
}

