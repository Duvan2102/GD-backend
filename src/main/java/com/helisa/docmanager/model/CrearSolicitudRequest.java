package com.helisa.docmanager.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearSolicitudRequest {

    @NotNull(message = "ID del solicitante es obligatorio")
    @Positive(message = "ID del solicitante debe ser positivo")
    private Integer idSolicitante;

    @NotNull(message = "ID de tipología es obligatorio")
    @Positive(message = "ID de tipología debe ser positivo")
    private Integer idTipologia;

    @NotNull(message = "Lista de destinatarios es obligatoria")
    @NotEmpty(message = "Debe incluir al menos un destinatario")
    private Integer[] destinatarios;

    @NotNull(message = "Orden de firma es obligatorio")
    private Boolean ordenFirma;

    @NotNull(message = "El nombre de la solicitud es obligatorio")
    private String nombreSolicitud;

    private String comentarioInicial;

    @NotNull(message = "PDF principal es obligatorio")
    private MultipartFile pdfPrincipal;

    private MultipartFile[] adjuntos;
}