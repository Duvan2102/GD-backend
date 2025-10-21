package com.helisa.docmanager.controller;

import com.helisa.docmanager.security.JwtService;
import com.helisa.docmanager.model.Cargo;
import com.helisa.docmanager.model.Estado;
import com.helisa.docmanager.model.RevokedToken;
import com.helisa.docmanager.model.Rol;
import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.model.Tipologia;
import com.helisa.docmanager.repository.CargoRepository;
import com.helisa.docmanager.repository.EstadoRepository;
import com.helisa.docmanager.repository.RevokedTokenRepository;
import com.helisa.docmanager.repository.RolRepository;
import com.helisa.docmanager.repository.UsuarioRepository;
import com.helisa.docmanager.repository.TipologiaRepository;
import com.helisa.docmanager.service.TwoFactorAuthService;
import com.helisa.docmanager.service.LoginAttemptService;
import com.helisa.docmanager.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private UsuarioService usuarioService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        
        try {
            // Verificar si el usuario está bloqueado
            if (loginAttemptService.isUserLocked(request.getUsuario())) {
                long lockoutTime = loginAttemptService.getLockoutTimeRemaining(request.getUsuario());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new ErrorResponse("USUARIO_BLOQUEADO", 
                                "Usuario bloqueado. Intenta en " + lockoutTime + " minutos."));
            }

            // Verificar límite de intentos por hora
            if (loginAttemptService.hasExceededHourlyLimit(request.getUsuario())) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new ErrorResponse("LIMITE_EXCEDIDO", 
                                "Has excedido el límite de intentos por hora."));
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsuario(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Usuario usuario = usuarioRepository.findByUsuario(request.getUsuario())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            loginAttemptService.recordSuccessfulLogin(request.getUsuario(), ipAddress);

            String tempToken = jwtService.generateTempToken(userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new TwoFactorRequiredResponse("Se requiere código de verificación", 
                            usuario.getUsuario(), true, tempToken));
            
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailedLogin(request.getUsuario(), ipAddress);
            int remainingAttempts = loginAttemptService.getRemainingAttempts(request.getUsuario());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("CREDENCIALES_INVALIDAS", 
                            "Usuario o contraseña incorrectos. Intentos restantes: " + remainingAttempts));
        } catch (Exception e) {
            loginAttemptService.recordFailedLogin(request.getUsuario(), ipAddress);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_INTERNO", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Validar que el usuario no exista
            if (usuarioRepository.existsByUsuario(request.getUsuario())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("USUARIO_EXISTE", "El nombre de usuario ya existe"));
            }

            // Validar que la identificación no exista
            if (usuarioRepository.existsByIdentificacion(request.getIdentificacion())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("IDENTIFICACION_EXISTE", "Ya existe un usuario con esta identificación"));
            }

            // Validar que el correo no exista
            if (request.getCorreoEmpresarial() != null && 
                usuarioRepository.findByCorreoEmpresarial(request.getCorreoEmpresarial()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("CORREO_EXISTE", "Ya existe un usuario con este correo empresarial"));
            }

            // Crear nuevo usuario
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setIdentificacion(request.getIdentificacion());
            nuevoUsuario.setNombres(request.getNombres());
            nuevoUsuario.setApellidos(request.getApellidos());
            nuevoUsuario.setUsuario(request.getUsuario());
            nuevoUsuario.setPassword(request.getPassword());
            nuevoUsuario.setCorreoEmpresarial(request.getCorreoEmpresarial());
            nuevoUsuario.setCorreoPersonal(request.getCorreoPersonal());
            nuevoUsuario.setTelefono1(request.getTelefono1());
            nuevoUsuario.setTelefono2(request.getTelefono2());
            nuevoUsuario.setDireccion(request.getDireccion());
          

            
            // Establecer cargo enviado por el usuario
            CargoRepository cargoRepository = usuarioService.getCargoRepository();
            Cargo cargoSeleccionado = cargoRepository.findById(request.getCargoId())
                    .orElseThrow(() -> new RuntimeException("Cargo con ID " + request.getCargoId() + " no encontrado"));
            nuevoUsuario.setCargo(cargoSeleccionado);

            // Establecer rol por defecto con ID 2
            RolRepository rolRepository = usuarioService.getRolRepository();
            Rol rolDefault = rolRepository.findById(2)
                    .orElseThrow(() -> new RuntimeException("Rol con ID 2 no encontrado en el sistema"));
            nuevoUsuario.setRol(rolDefault);

            // Establecer estado PENDIENTE
            EstadoRepository estadoRepository = usuarioService.getEstadoRepository();
            Estado estadoPendiente = estadoRepository.findByDescripcion("PENDIENTE")
                    .orElseThrow(() -> new RuntimeException("Estado PENDIENTE no encontrado en la base de datos"));
            nuevoUsuario.setEstado(estadoPendiente);

            // Crear usuario usando el servicio
            Usuario usuarioCreado = usuarioService.crearUsuario(nuevoUsuario);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RegisterResponse("Usuario registrado exitosamente. Debe ser activado por un administrador.", 
                            usuarioCreado.getIdUsuario()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_REGISTRO", e.getMessage()));
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
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("NO_AUTENTICADO", "Usuario no autenticado"));
            }

            String username = authentication.getName();
            Usuario usuario = usuarioRepository.findByUsuario(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

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
        private String codigo2FA; // Código de doble autenticación
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

    @Data
    public static class TwoFactorRequiredResponse {
        private final String message;
        private final String usuario;
        private final boolean requiere2FA;
        private final String tempToken; // Token temporal para validar 2FA
    }

    @Data
    public static class TwoFactorSetupResponse {
        private final String qrCodeUrl;
        private final String secret;
        private final String message;
    }

    // ========== NUEVOS ENDPOINTS PARA DOBLE AUTENTICACIÓN ==========

    /**
     * Valida el código de doble autenticación y genera JWT final
     */
    @PostMapping("/validate-2fa")
    public ResponseEntity<?> validateTwoFactor(@Valid @RequestBody Validate2FARequest request) {
        try {
            // Validar token temporal
            if (!jwtService.isTempTokenValid(request.getTempToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("TOKEN_INVALIDO", "Token temporal inválido o expirado"));
            }

            String username = jwtService.extractUsernameFromTempToken(request.getTempToken());
            Usuario usuario = usuarioRepository.findByUsuario(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Verificar si necesita configurar Google Auth
            if (usuario.getTokenQr() != null && usuario.getTokenQr() &&
                twoFactorAuthService.hasGoogleAuthConfigured(usuario.getUsuario()) && 
                !twoFactorAuthService.isGoogleAuthConfirmed(usuario.getUsuario())) {
                // El usuario tiene Google Auth configurado pero no confirmado
                String existingSecret = twoFactorAuthService.getExistingGoogleAuthSecret(usuario.getUsuario());
                String qrCodeUrl = twoFactorAuthService.generateQRCodeUrl(existingSecret, usuario.getUsuario(), "Helisa");
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                        .body(new TwoFactorSetupResponse(qrCodeUrl, existingSecret, 
                                "Debes configurar Google Authenticator escaneando el código QR y luego confirma con un código"));
            }

            // Validar código 2FA según método configurado
            if (!twoFactorAuthService.validateTwoFactorCode(usuario, request.getCodigo2FA())) {
                String metodo = (usuario.getTokenQr() != null && usuario.getTokenQr()) ? "Google Authenticator" : "email";
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("CODIGO_2FA_INVALIDO", "Código de " + metodo + " incorrecto"));
            }

            // Generar JWT completo
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    usuario.getUsuario(), usuario.getPassword(), java.util.Collections.emptyList());
            String finalToken = jwtService.generateToken(userDetails);
            
            return ResponseEntity.ok(new AuthResponse(finalToken, usuario, tipologiaRepository));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_VALIDACION", e.getMessage()));
        }
    }

    /**
     * Envía código de verificación por email
     */
    @PostMapping("/send-email-code")
    public ResponseEntity<?> sendEmailCode(@Valid @RequestBody SendCodeRequest request) {
        try {
            Usuario usuario = usuarioRepository.findByUsuario(request.getUsuario())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (usuario.getTokenCorreo() == null || !usuario.getTokenCorreo()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("2FA_METHOD_INCORRECT", "Tu método de 2FA es Google Authenticator, no email"));
            }

            twoFactorAuthService.sendEmailCode(usuario);
            return ResponseEntity.ok(new MessageResponse("Código enviado por email"));
        } catch (RuntimeException e) {
            // Verificar si es el error de código ya existente
            if (e.getMessage().contains("Ya existe un código válido")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new ErrorResponse("CODIGO_EXISTENTE", e.getMessage()));
            } else if (e.getMessage().contains("excedido el límite")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new ErrorResponse("LIMITE_EXCEDIDO", e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse("ERROR_ENVIO", e.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_ENVIO", e.getMessage()));
        }
    }

    /**
     * Configura Google Authenticator para un usuario (PRIMERA VEZ)
     */
    @PostMapping("/setup-google-auth")
    public ResponseEntity<?> setupGoogleAuth(@Valid @RequestBody SetupGoogleAuthRequest request) {
        try {
            Usuario usuario = usuarioRepository.findByUsuario(request.getUsuario())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Cambiar a método Google Auth
            usuario.setTokenQr(true);
            usuario.setTokenCorreo(false);
            usuarioRepository.save(usuario);

            TwoFactorAuthService.TwoFactorSetupResult result = twoFactorAuthService.setupGoogleAuth(usuario);
            
            return ResponseEntity.ok(new TwoFactorSetupResponse(result.getQrCodeUrl(), result.getSecret(), 
                    "Escanea el código QR con Google Authenticator y luego confirma con un código"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_SETUP", e.getMessage()));
        }
    }

    /**
     * Confirma la configuración de Google Authenticator con un código
     */
    @PostMapping("/confirm-google-auth")
    public ResponseEntity<?> confirmGoogleAuth(@Valid @RequestBody ConfirmGoogleAuthRequest request) {
        try {
            Usuario usuario = usuarioRepository.findByUsuario(request.getUsuario())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Validar el código de Google Authenticator
            if (!twoFactorAuthService.validateGoogleAuthCode(request.getSecret(), Integer.parseInt(request.getCodigo()))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("CODIGO_INVALIDO", "Código de Google Authenticator incorrecto"));
            }

            // Eliminar token pendiente
            twoFactorAuthService.removePendingGoogleAuth(request.getUsuario());
            
            // Verificar que usuario tiene Google Auth activo
            if (usuario.getTokenQr() == null || !usuario.getTokenQr()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("2FA_METHOD_INCORRECT", "Google Auth no está configurado como tu método activo"));
            }

            return ResponseEntity.ok(new MessageResponse("Google Authenticator configurado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_CONFIRMACION", e.getMessage()));
        }
    }

    // Endpoint /disable-2fa eliminado - 2FA es obligatorio para todos los usuarios

    /**
     * Obtiene estadísticas de intentos de login
     */
    @GetMapping("/login-stats/{usuario}")
    public ResponseEntity<?> getLoginStats(@PathVariable String usuario) {
        try {
            String stats = loginAttemptService.getAttemptStats(usuario);
            return ResponseEntity.ok(new MessageResponse(stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_STATS", e.getMessage()));
        }
    }

    /**
     * Desbloquea un usuario manualmente
     */
    @PostMapping("/unlock-user/{usuario}")
    public ResponseEntity<?> unlockUser(@PathVariable String usuario) {
        try {
            loginAttemptService.unlockUser(usuario);
            return ResponseEntity.ok(new MessageResponse("Usuario desbloqueado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_UNLOCK", e.getMessage()));
        }
    }

    /**
     * Obtiene el estado de configuración de 2FA para un usuario
     */
    @GetMapping("/2fa-status/{usuario}")
    @CrossOrigin(origins = "*")
    public ResponseEntity<?> get2FAStatus(@PathVariable String usuario) {
        try {
            Usuario user = usuarioRepository.findByUsuario(usuario)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Obtener configuración actual de 2FA
            boolean hasGoogleAuth = twoFactorAuthService.hasGoogleAuthConfigured(usuario);
            boolean isGoogleAuthConfirmed = twoFactorAuthService.isGoogleAuthConfirmed(usuario);
            boolean tokenCorreo = user.getTokenCorreo() != null && user.getTokenCorreo();
            
            // Si tiene Google Auth configurado pero no confirmado, considerarlo como configurado
            boolean googleAuthConfigured = hasGoogleAuth && isGoogleAuthConfirmed;
            boolean googleAuthPending = hasGoogleAuth && !isGoogleAuthConfirmed;
            
            return ResponseEntity.ok(new TwoFactorStatusResponse(
                googleAuthConfigured, 
                tokenCorreo,
                googleAuthPending,
                "Estado de configuración 2FA"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_INTERNO", e.getMessage()));
        }
    }

    /**
     * Obtiene o regenera el código QR para Google Authenticator
     */
    @PostMapping("/get-google-auth-qr")
    public ResponseEntity<?> getGoogleAuthQR(@Valid @RequestBody SetupGoogleAuthRequest request) {
        try {
            Usuario usuario = usuarioRepository.findByUsuario(request.getUsuario())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Verificar si ya tiene Google Auth configurado
            if (twoFactorAuthService.hasGoogleAuthConfigured(request.getUsuario())) {
                // Si ya tiene configurado, devolver el QR existente
                String existingSecret = twoFactorAuthService.getExistingGoogleAuthSecret(request.getUsuario());
                String qrCodeUrl = twoFactorAuthService.generateQRCodeUrl(existingSecret, request.getUsuario(), "Helisa");
                return ResponseEntity.ok(new TwoFactorSetupResponse(qrCodeUrl, existingSecret, 
                        "Código QR existente para Google Authenticator"));
            } else {
                // Si no tiene configurado, generar uno nuevo
                TwoFactorAuthService.TwoFactorSetupResult result = twoFactorAuthService.setupGoogleAuth(usuario);
                return ResponseEntity.ok(new TwoFactorSetupResponse(result.getQrCodeUrl(), result.getSecret(), 
                        "Nuevo código QR para Google Authenticator"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_QR_GENERATION", e.getMessage()));
        }
    }

    /**
     * Desvincula Google Authenticator del usuario
     */
    @PostMapping("/remove-google-auth")
    public ResponseEntity<?> removeGoogleAuth(@Valid @RequestBody RemoveGoogleAuthRequest request) {
        try {
            Usuario usuario = usuarioRepository.findByUsuario(request.getUsuario())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("PASSWORD_INCORRECT", "Contraseña incorrecta"));
            }
            
            // Eliminar Google Auth actual
            twoFactorAuthService.removeGoogleAuth(request.getUsuario());
            
            // Si el usuario tiene 2FA habilitado, generar nuevo setup para que pueda reconfigurar
            // 2FA es obligatorio - siempre generar nuevo setup para Google Auth
            twoFactorAuthService.setupGoogleAuth(usuario);
            return ResponseEntity.ok(new MessageResponse("Google Authenticator desvinculado. Debes configurar Google Authenticator nuevamente en tu próximo inicio de sesión."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_REMOVE_GOOGLE_AUTH", e.getMessage()));
        }
    }

    /**
     * Limpia el estado de 2FA de un usuario (para corregir inconsistencias)
     */
    @PostMapping("/reset-2fa-state/{usuario}")
    public ResponseEntity<?> reset2FAState(@PathVariable String usuario) {
        try {
            Usuario user = usuarioRepository.findByUsuario(usuario)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Limpiar todos los tokens de 2FA
            twoFactorAuthService.removeGoogleAuth(usuario);
            
            // Si el usuario tenía 2FA habilitado, generar nuevo setup para Google Auth
            // 2FA es obligatorio - siempre generar nuevo setup para Google Auth
            twoFactorAuthService.setupGoogleAuth(user);
            return ResponseEntity.ok(new MessageResponse("Estado de 2FA limpiado. El usuario debe configurar Google Authenticator en su próximo inicio de sesión."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_RESET", e.getMessage()));
        }
    }

    /**
     * Cambia el método de autenticación 2FA del usuario
     */
    @PostMapping("/change-2fa-method")
    public ResponseEntity<?> change2FAMethod(@Valid @RequestBody Change2FAMethodRequest request) {
        try {
            // Buscar usuario por ID
            Usuario usuario = usuarioRepository.findById(request.getIdUsuario())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // 2FA es obligatorio para todos los usuarios - no necesita verificación

            String nuevoMetodo = request.getNuevoMetodo().toUpperCase();
            
            // Validar que el método sea válido
            if (!"EMAIL".equals(nuevoMetodo) && !"GOOGLE_AUTH".equals(nuevoMetodo)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("METODO_INVALIDO", "El método debe ser 'EMAIL' o 'GOOGLE_AUTH'"));
            }

            // Cambiar el método según la solicitud
            if ("EMAIL".equals(nuevoMetodo)) {
                // Cambiar a método Email
                usuario.setTokenCorreo(true);
                usuario.setTokenQr(false);
                usuarioRepository.save(usuario);
                
                // Eliminar Google Auth si existe
                if (twoFactorAuthService.hasGoogleAuthConfigured(usuario.getUsuario())) {
                    twoFactorAuthService.removeGoogleAuth(usuario.getUsuario());
                }
                
                return ResponseEntity.ok(new MessageResponse("Método de autenticación cambiado a EMAIL exitosamente"));
                
            } else if ("GOOGLE_AUTH".equals(nuevoMetodo)) {
                // Cambiar a método Google Auth
                usuario.setTokenQr(true);
                usuario.setTokenCorreo(false);
                usuarioRepository.save(usuario);
                
                // Generar nueva configuración para Google Auth
                twoFactorAuthService.setupGoogleAuth(usuario);
                
                return ResponseEntity.ok(new MessageResponse("Método de autenticación cambiado a GOOGLE_AUTH. " +
                        "Debes configurar Google Authenticator en tu próximo inicio de sesión."));
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_INTERNO", "Error interno del servidor"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("ERROR_CAMBIO_METODO", e.getMessage()));
        }
    }

    // ========== CLASES DE REQUEST ==========

    @Data
    public static class SendCodeRequest {
        private String usuario;
    }

    @Data
    public static class SetupGoogleAuthRequest {
        private String usuario;
    }

    @Data
    public static class Disable2FARequest {
        private String usuario;
        private String password;
    }

    @Data
    public static class Validate2FARequest {
        private String tempToken;
        private String codigo2FA;
    }

    @Data
    public static class ConfirmGoogleAuthRequest {
        private String usuario;
        private String secret;
        private String codigo;
    }

    @Data
    public static class TwoFactorStatusResponse {
        private boolean hasGoogleAuth;
        private boolean hasEmailBackup;
        private boolean googleAuthPending;
        private String message;
        
        public TwoFactorStatusResponse(boolean hasGoogleAuth, boolean hasEmailBackup, boolean googleAuthPending, String message) {
            this.hasGoogleAuth = hasGoogleAuth;
            this.hasEmailBackup = hasEmailBackup;
            this.googleAuthPending = googleAuthPending;
            this.message = message;
        }
    }

    @Data
    public static class RemoveGoogleAuthRequest {
        private String usuario;
        private String password;
    }

    @Data
    public static class Change2FAMethodRequest {
        private Integer idUsuario;
        private String nuevoMetodo;
    }

    @Data
    public static class RegisterRequest {
        private String identificacion;
        private String nombres;
        private String apellidos;
        private String usuario;
        private String password;
        private String correoEmpresarial;
        private String correoPersonal;
        private String telefono1;
        private String telefono2;
        private String direccion;
        private Integer cargoId;
    }

    @Data
    public static class RegisterResponse {
        private final String message;
        private final Integer idUsuario;
        
        public RegisterResponse(String message, Integer idUsuario) {
            this.message = message;
            this.idUsuario = idUsuario;
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
