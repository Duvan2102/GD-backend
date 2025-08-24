package com.helisa.docmanager.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdjuntoResponse {
    private Long id;
    private String originalName;
    private Long sizeBytes;
    private String mime;
}
