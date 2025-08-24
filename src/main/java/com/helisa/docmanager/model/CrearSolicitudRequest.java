package com.helisa.docmanager.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CrearSolicitudRequest {

    @NotNull(message = "El ID del solicitante es obligatorio")
    @Positive(message = "El ID del solicitante debe ser positivo")
    private Long idSolicitante;

    @NotNull(message = "El ID de tipología es obligatorio")
    @Positive(message = "El ID de tipología debe ser positivo")
    private Long tipologiaId;

    @NotEmpty(message = "La lista de destinatarios no puede estar vacía")
    private Long[] destinatarios;

    @NotNull(message = "El orden de firma es obligatorio")
    private Boolean ordenFirma;

    @Size(max = 500, message = "El comentario inicial no puede exceder 500 caracteres")
    private String comentarioInicial;

    @NotNull(message = "El PDF principal es obligatorio")
    private MultipartFile pdfPrincipal;

    private MultipartFile[] adjuntos;
}
