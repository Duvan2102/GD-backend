package com.helisa.docmanager.controller;

import com.helisa.docmanager.security.JwtService;
import com.helisa.docmanager.model.RevokedToken;
import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.model.Tipologia;
import com.helisa.docmanager.repository.RevokedTokenRepository;
import com.helisa.docmanager.repository.UsuarioRepository;
import com.helisa.docmanager.repository.TipologiaRepository;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TipologiaRepository tipologiaRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsuario(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);
            
            // Obtener datos completos del usuario
            Usuario usuario = usuarioRepository.findByUsuario(request.getUsuario())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            return ResponseEntity.ok(new AuthResponse(token, usuario, tipologiaRepository));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("CREDENCIALES_INVALIDAS", "Usuario o contraseña incorrectos"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_INTERNO", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("TOKEN_REQUERIDO", "Se requiere encabezado Authorization Bearer"));
            }
            String token = authHeader.substring(7);

            String jti = jwtService.extractJti(token);
            var exp = jwtService.extractExpiration(token);

            RevokedToken rt = new RevokedToken();
            rt.setJti(jti);
            rt.setExpiresAt(exp.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            revokedTokenRepository.save(rt);

            return ResponseEntity.ok(new MessageResponse("Logout exitoso"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("TOKEN_INVALIDO", "No fue posible procesar el token"));
        }
    }

    @PostMapping("/validate-password")
    public ResponseEntity<?> validatePassword(@Valid @RequestBody PasswordValidationRequest request) {
        try {
            // Obtener el usuario autenticado desde el contexto de seguridad
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("NO_AUTENTICADO", "Usuario no autenticado"));
            }

            String username = authentication.getName();
            Usuario usuario = usuarioRepository.findByUsuario(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Validar la contraseña
            boolean isPasswordValid = passwordEncoder.matches(request.getPassword(), usuario.getPassword());
            
            if (isPasswordValid) {
                return ResponseEntity.ok(new PasswordValidationResponse(true, "Contraseña válida"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new PasswordValidationResponse(false, "Contraseña incorrecta"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_INTERNO", e.getMessage()));
        }
    }

    @Data
    public static class LoginRequest {
        private String usuario;
        private String password;
    }

    @Data
    public static class AuthResponse {
        private final String token;
        private final UsuarioData usuario;
        
        public AuthResponse(String token, Usuario usuario, TipologiaRepository tipologiaRepository) {
            this.token = token;
            this.usuario = new UsuarioData(usuario, tipologiaRepository);
        }
        
        @Data
        public static class UsuarioData {
            private final Integer idUsuario;
            private final String identificacion;
            private final String nombres;
            private final String apellidos;
            private final String usuario;
            private final String correoEmpresarial;
            private final String correoPersonal;
            private final String telefono1;
            private final String telefono2;
            private final String direccion;
            private final CargoData cargo;
            private final String rol;
            private final String estado;
            private final java.util.List<TipologiaData> tipologias;
            
            public UsuarioData(Usuario usuario, TipologiaRepository tipologiaRepository) {
                this.idUsuario = usuario.getIdUsuario();
                this.identificacion = usuario.getIdentificacion();
                this.nombres = usuario.getNombres();
                this.apellidos = usuario.getApellidos();
                this.usuario = usuario.getUsuario();
                this.correoEmpresarial = usuario.getCorreoEmpresarial();
                this.correoPersonal = usuario.getCorreoPersonal();
                this.telefono1 = usuario.getTelefono1();
                this.telefono2 = usuario.getTelefono2();
                this.direccion = usuario.getDireccion();
                this.cargo = usuario.getCargo() != null ? new CargoData(usuario.getCargo()) : null;
                this.rol = usuario.getRol() != null ? usuario.getRol().getDescripcion() : null;
                this.estado = usuario.getEstado() != null ? usuario.getEstado().getDescripcion() : null;
                
                // Obtener tipologías asociadas al cargo del usuario
                if (usuario.getCargo() != null) {
                    this.tipologias = tipologiaRepository.findByCargoIdCargo(usuario.getCargo().getIdCargo())
                            .stream()
                            .map(TipologiaData::new)
                            .collect(java.util.stream.Collectors.toList());
                } else {
                    this.tipologias = java.util.Collections.emptyList();
                }
            }
            
            @Data
            public static class CargoData {
                private final Integer idCargo;
                private final String descripcion;
                private final String area;
                private final String departamento;
                
                public CargoData(com.helisa.docmanager.model.Cargo cargo) {
                    this.idCargo = cargo.getIdCargo();
                    this.descripcion = cargo.getDescripcion();
                    this.area = cargo.getArea() != null ? cargo.getArea().getDescripcion() : null;
                    this.departamento = cargo.getArea() != null && cargo.getArea().getDepartamento() != null 
                            ? cargo.getArea().getDepartamento().getDescripcion() : null;
                }
            }
            
            @Data
            public static class TipologiaData {
                private final Integer idTipologia;
                private final String descripcion;
                
                public TipologiaData(Tipologia tipologia) {
                    this.idTipologia = tipologia.getIdTipologia();
                    this.descripcion = tipologia.getDescripcion();
                }
            }
        }
    }

    @Data
    public static class ErrorResponse {
        private final String code;
        private final String message;
    }

    @Data
    public static class MessageResponse {
        private final String message;
    }

    @Data
    public static class PasswordValidationRequest {
        private String password;
    }

    @Data
    public static class PasswordValidationResponse {
        private final boolean valid;
        private final String message;

        public PasswordValidationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }
}
