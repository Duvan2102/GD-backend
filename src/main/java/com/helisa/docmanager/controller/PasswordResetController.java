package com.helisa.docmanager.controller;

import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.Data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/password")
@CrossOrigin(origins = "*")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/request-reset")
    public ResponseEntity<?> solicitarRestablecimiento(@Valid @RequestBody PasswordResetRequest request) {
        try {
            if ((request.getEmail() == null || request.getEmail().trim().isEmpty()) && 
                (request.getUsername() == null || request.getUsername().trim().isEmpty())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("CAMPOS_REQUERIDOS", "Debe proporcionar un correo electrónico o nombre de usuario"));
            }
            
            passwordResetService.solicitarRestablecimiento(request.getEmail(), request.getUsername());
            
            return ResponseEntity.ok(new MessageResponse(
                "Si el correo electrónico o nombre de usuario existe en nuestro sistema, recibirá instrucciones para restablecer su contraseña."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_SOLICITUD", e.getMessage()));
        }
    }

    @GetMapping("/validate-token")
    public ResponseEntity<?> validarToken(@RequestParam String token) {
        try {
            boolean esValido = passwordResetService.validarToken(token);
            return ResponseEntity.ok(new TokenValidationResponse(esValido));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_VALIDACION", e.getMessage()));
        }
    }

    @GetMapping("/user-info")
    public ResponseEntity<?> obtenerInformacionUsuario(@RequestParam String token) {
        try {
            Usuario usuario = passwordResetService.obtenerUsuarioPorToken(token);
            
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("TOKEN_INVALIDO", "Token inválido o expirado"));
            }

            return ResponseEntity.ok(new UserInfoResponse(
                usuario.getIdUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getUsuario(),
                usuario.getCorreoEmpresarial()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_OBTENCION", e.getMessage()));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> restablecerPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            boolean exitoso = passwordResetService.restablecerPassword(request.getToken(), request.getNewPassword());
            
            if (exitoso) {
                return ResponseEntity.ok(new MessageResponse("Contraseña restablecida exitosamente"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("TOKEN_INVALIDO", "Token inválido, expirado o contraseña no válida"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_RESTABLECIMIENTO", e.getMessage()));
        }
    }

    @Data
    public static class PasswordResetRequest {
        private String email;
        private String username;
    }

    @Data
    public static class PasswordResetConfirmRequest {
        private String token;
        private String newPassword;
    }

    @Data
    public static class MessageResponse {
        private final String message;
        
        public MessageResponse(String message) {
            this.message = message;
        }
    }

    @Data
    public static class ErrorResponse {
        private final String code;
        private final String message;
    }

    @Data
    public static class TokenValidationResponse {
        private final boolean valid;
        
        public TokenValidationResponse(boolean valid) {
            this.valid = valid;
        }
    }

    @Data
    public static class UserInfoResponse {
        private final Integer idUsuario;
        private final String nombres;
        private final String apellidos;
        private final String usuario;
        private final String correoEmpresarial;
        
        public UserInfoResponse(Integer idUsuario, String nombres, String apellidos, 
                              String usuario, String correoEmpresarial) {
            this.idUsuario = idUsuario;
            this.nombres = nombres;
            this.apellidos = apellidos;
            this.usuario = usuario;
            this.correoEmpresarial = correoEmpresarial;
        }
    }
}

