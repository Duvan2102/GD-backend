package com.helisa.docmanager.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("VALIDATION_ERROR")
                .message("Errores de validación en los datos enviados")
                .details(details)
                .build();

        log.warn("Errores de validación en {}: {}", request.getRequestURI(), details);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("VALIDATION_ERROR")
                .message("Violación de restricciones de datos")
                .details(details)
                .build();

        log.warn("Violación de constraints en {}: {}", request.getRequestURI(), details);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("NOT_FOUND")
                .message(ex.getMessage())
                .build();

        log.warn("Entidad no encontrada en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("CONFLICT")
                .message(ex.getMessage())
                .build();

        log.warn("Estado ilegal en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        String message = ex.getMessage();
        String code = "VALIDATION_ERROR";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // Detectar tipo específico de error para código y status apropiados
        if (message != null) {
            String lowerMessage = message.toLowerCase();

            // Errores de tipo de archivo
            if (lowerMessage.contains("extensión") ||
                    lowerMessage.contains("pdf") && lowerMessage.contains("solo") ||
                    lowerMessage.contains("tipo") && lowerMessage.contains("archivo")) {
                code = "UNSUPPORTED_MEDIA_TYPE";
                status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            }
            // Errores de tamaño
            else if (lowerMessage.contains("tamaño") ||
                    lowerMessage.contains("excede") ||
                    lowerMessage.contains("máximo")) {
                code = "PAYLOAD_TOO_LARGE";
                status = HttpStatus.PAYLOAD_TOO_LARGE;
            }
            // Errores de autorización
            else if (lowerMessage.contains("autorizado") ||
                    lowerMessage.contains("permiso")) {
                code = "FORBIDDEN";
                status = HttpStatus.FORBIDDEN;
            }
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code(code)
                .message(message != null ? message : "Argumento inválido")
                .build();

        log.warn("Argumento ilegal en {} [{}]: {}", request.getRequestURI(), code, message);
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileSizeLimit(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        String maxSize = ex.getMaxUploadSize() > 0 ?
                (ex.getMaxUploadSize() / 1024 / 1024) + "MB" : "configurado";

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("PAYLOAD_TOO_LARGE")
                .message("El archivo excede el tamaño máximo permitido de " + maxSize)
                .build();

        log.warn("Archivo demasiado grande en {}: maxSize={}", request.getRequestURI(), maxSize);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        List<String> details = new ArrayList<>();
        if (ex.getContentType() != null) {
            details.add("Tipo de contenido no soportado: " + ex.getContentType());
        }
        if (!ex.getSupportedMediaTypes().isEmpty()) {
            details.add("Tipos soportados: " + ex.getSupportedMediaTypes().toString());
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("UNSUPPORTED_MEDIA_TYPE")
                .message("Tipo de contenido no soportado")
                .details(details)
                .build();

        log.warn("Media type no soportado en {}: {}", request.getRequestURI(), ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(
            MultipartException ex, HttpServletRequest request) {

        String message = "Error al procesar archivo multipart";
        String code = "BAD_REQUEST";

        // Detectar si es problema de tamaño
        if (ex.getCause() instanceof MaxUploadSizeExceededException) {
            message = "Archivo demasiado grande";
            code = "PAYLOAD_TOO_LARGE";
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ErrorResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .path(request.getRequestURI())
                            .code(code)
                            .message(message)
                            .build());
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code(code)
                .message(message)
                .build();

        log.warn("Error multipart en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("FORBIDDEN")
                .message("Acceso denegado. No tiene permisos para realizar esta acción")
                .build();

        log.warn("Acceso denegado en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("NOT_FOUND")
                .message("Endpoint no encontrado: " + ex.getHttpMethod() + " " + ex.getRequestURL())
                .build();

        log.warn("Endpoint no encontrado: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        String message = "Formato de datos inválido en el cuerpo de la petición";

        // Detectar errores específicos de JSON
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("JSON")) {
                message = "Error en formato JSON. Verifique la sintaxis";
            } else if (ex.getMessage().contains("Required request body is missing")) {
                message = "El cuerpo de la petición es obligatorio";
            }
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("BAD_REQUEST")
                .message(message)
                .build();

        log.warn("Mensaje no legible en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("BAD_REQUEST")
                .message("Parámetro obligatorio faltante: " + ex.getParameterName())
                .build();

        log.warn("Parámetro faltante en {}: {}", request.getRequestURI(), ex.getParameterName());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format("Tipo de datos incorrecto para parámetro '%s'. " +
                        "Esperado: %s, Recibido: %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                ex.getValue());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("BAD_REQUEST")
                .message(message)
                .build();

        log.warn("Tipo de dato incorrecto en {}: {}", request.getRequestURI(), message);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {

        // Logging con más detalle para RuntimeExceptions
        log.error("RuntimeException en {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("INTERNAL_SERVER_ERROR")
                .message("Error interno del servidor. Contacte al administrador")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {

        // Log completo para excepciones no controladas
        log.error("Excepción no controlada en {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .code("INTERNAL_SERVER_ERROR")
                .message("Error interno del servidor")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
