package com.helisa.docmanager.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
class ErrorResponse {
    private LocalDateTime timestamp;
    private String path;
    private String code;
    private String message;
    private List<String> details;
}