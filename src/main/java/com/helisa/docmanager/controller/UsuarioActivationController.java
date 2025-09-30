package com.helisa.docmanager.controller;

import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.service.UsuarioActivationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioActivationController {

    @Autowired
    private UsuarioActivationService usuarioActivationService;

    /**
     * Obtiene todos los usuarios pendientes
     * @return Lista de usuarios pendientes
     */
    @GetMapping("/pendientes")
    public ResponseEntity<?> obtenerUsuariosPendientes() {
        try {
            List<Usuario> usuariosPendientes = usuarioActivationService.obtenerUsuariosPendientes();
            return ResponseEntity.ok(usuariosPendientes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_INTERNO", e.getMessage()));
        }
    }

    /**
     * Activa un usuario pendiente o inactivo
     * - Si está PENDIENTE: se le enviará un correo para que cree su contraseña
     * - Si está INACTIVO: se le enviará un correo notificando su reactivación
     * @param idUsuario ID del usuario a activar
     * @return Usuario activado
     */
    @PostMapping("/{idUsuario}/activar")
    public ResponseEntity<?> activarUsuario(@PathVariable Integer idUsuario) {
        try {
            Usuario usuarioActivado = usuarioActivationService.activarUsuario(idUsuario);
            
            String mensaje = "Usuario activado exitosamente";
            if (usuarioActivado.getCorreoEmpresarial() != null && !usuarioActivado.getCorreoEmpresarial().trim().isEmpty()) {
                mensaje += ". Se ha enviado un correo electrónico con las instrucciones";
            }
            
            return ResponseEntity.ok(new ActivationResponse(mensaje, usuarioActivado));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("ESTADO_INVALIDO", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("USUARIO_NO_ENCONTRADO", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_ACTIVACION", e.getMessage()));
        }
    }

    /**
     * Rechaza un usuario pendiente
     * @param idUsuario ID del usuario a rechazar
     * @return Usuario rechazado
     */
    @PostMapping("/{idUsuario}/rechazar")
    public ResponseEntity<?> rechazarUsuario(@PathVariable Integer idUsuario) {
        try {
            Usuario usuarioRechazado = usuarioActivationService.rechazarUsuario(idUsuario);
            return ResponseEntity.ok(new ActivationResponse("Usuario rechazado", usuarioRechazado));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("ESTADO_INVALIDO", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("USUARIO_NO_ENCONTRADO", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_RECHAZO", e.getMessage()));
        }
    }

    /**
     * Verifica si un usuario está pendiente
     * @param idUsuario ID del usuario
     * @return true si está pendiente, false en caso contrario
     */
    @GetMapping("/{idUsuario}/estado-pendiente")
    public ResponseEntity<?> verificarEstadoPendiente(@PathVariable Integer idUsuario) {
        try {
            boolean esPendiente = usuarioActivationService.esUsuarioPendiente(idUsuario);
            return ResponseEntity.ok(new EstadoPendienteResponse(esPendiente));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_VERIFICACION", e.getMessage()));
        }
    }

    // Clases de respuesta
    @lombok.Data
    public static class ErrorResponse {
        private final String code;
        private final String message;
    }

    @lombok.Data
    public static class ActivationResponse {
        private final String message;
        private final Usuario usuario;
        
        public ActivationResponse(String message, Usuario usuario) {
            this.message = message;
            this.usuario = usuario;
        }
    }

    @lombok.Data
    public static class EstadoPendienteResponse {
        private final boolean esPendiente;
        
        public EstadoPendienteResponse(boolean esPendiente) {
            this.esPendiente = esPendiente;
        }
    }
}

