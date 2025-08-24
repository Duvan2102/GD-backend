package com.helisa.docmanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjuntoResponse {
    private Long id;  // Long porque SolicitudAdjunto usa Long
    private String originalName;
    private Long sizeBytes;
    private String mime;
}
