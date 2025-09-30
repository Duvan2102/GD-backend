package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Token;
import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.repository.TokenRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class TwoFactorAuthService {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
    private final Random random = new Random();

    // Constantes para tipos de validación
    public static final String TIPO_GOOGLE_AUTH = "GOOGLE_AUTH";
    public static final String TIPO_EMAIL_CODE = "EMAIL_CODE";
    public static final String TIPO_GOOGLE_AUTH_SECRET = "GOOGLE_AUTH_SECRET";
    public static final String TIPO_GOOGLE_AUTH_PENDING = "GOOGLE_AUTH_PENDING";

    // Configuración desde properties
    @Value("${app.2fa.email-code.validity-minutes:5}")
    private int emailCodeValidityMinutes;
    
    @Value("${app.2fa.max-attempts-per-hour:5}")
    private int maxAttemptsPerHour;

    /**
     * Genera un código de 6 dígitos para envío por email
     */
    public String generateEmailCode() {
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * Genera un secreto para Google Authenticator
     */
    public String generateGoogleAuthSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    /**
     * Genera la URL QR para Google Authenticator
     */
    public String generateQRCodeUrl(String secret, String usuario, String issuer) {
        // Generar URL manualmente para evitar problemas de compatibilidad
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", 
                           issuer, usuario, secret, issuer);
    }

    /**
     * Valida un código de Google Authenticator
     */
    public boolean validateGoogleAuthCode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }

    /**
     * Valida un código de email
     * - Solo valida el código más reciente del usuario
     * - Elimina el código después de validarlo exitosamente
     * - No permite reutilizar el mismo código
     */
    @Transactional
    public boolean validateEmailCode(String codigo, Usuario usuario) {
        LocalDateTime now = LocalDateTime.now();
        
        // Buscar el token por código y tipo
        var tokenOpt = tokenRepository.findValidTokenByCodigoAndTipo(codigo, TIPO_EMAIL_CODE, now);
        
        if (tokenOpt.isEmpty()) {
            return false; // Código no encontrado o expirado
        }
        
        Token token = tokenOpt.get();
        
        // Verificar que el token pertenece al usuario correcto
        if (!token.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            return false; // El código no pertenece a este usuario
        }
        
        // Verificar que es el código más reciente del usuario
        // (prevenir uso de códigos antiguos que aún no han expirado)
        var tokensUsuario = tokenRepository.findValidTokensByUsuarioAndTipoOrderByFechaExpDesc(
            usuario, TIPO_EMAIL_CODE, now);
        
        if (!tokensUsuario.isEmpty() && !tokensUsuario.get(0).getIdToken().equals(token.getIdToken())) {
            // Existe un código más reciente, este ya no es válido
            return false;
        }
        
        // Código válido - eliminarlo inmediatamente para prevenir reutilización
        tokenRepository.delete(token);
        
        return true;
    }

    /**
     * Envía código de verificación por email
     * - Invalida todos los códigos anteriores del usuario
     * - Genera un nuevo código con tiempo de vida configurable
     * - Solo el último código generado será válido
     */
    @Transactional
    public Token sendEmailCode(Usuario usuario) {
        // Verificar límite de intentos
        if (hasExceededAttemptLimit(usuario, TIPO_EMAIL_CODE)) {
            throw new RuntimeException("Has excedido el límite de intentos. Intenta más tarde.");
        }

        // IMPORTANTE: Invalidar/eliminar todos los códigos de email anteriores del usuario
        // Solo debe existir UN código válido a la vez
        LocalDateTime now = LocalDateTime.now();
        List<Token> tokensPrevios = tokenRepository.findValidTokensByUsuarioAndTipoOrderByFechaExpDesc(
            usuario, TIPO_EMAIL_CODE, now);
        
        if (!tokensPrevios.isEmpty()) {
            // Eliminar códigos anteriores para que no se puedan usar
            tokenRepository.deleteAll(tokensPrevios);
        }

        // Generar nuevo código
        String codigo = generateEmailCode();
        
        // Crear token con tiempo de vida configurable
        Token token = new Token();
        token.setUsuario(usuario);
        token.setCodigo(codigo);
        token.setTipoValidacion(TIPO_EMAIL_CODE);
        token.setFechaExp(LocalDateTime.now().plusMinutes(emailCodeValidityMinutes));
        
        // Guardar token
        token = tokenRepository.save(token);
        
        // Enviar email
        String email = usuario.getCorreoEmpresarial() != null ? 
                      usuario.getCorreoEmpresarial() : usuario.getCorreoPersonal();
        
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("El usuario no tiene un email configurado");
        }
        
        emailService.sendTwoFactorCode(email, codigo, usuario.getNombres() + " " + usuario.getApellidos());
        
        return token;
    }

    /**
     * Configura Google Authenticator para un usuario
     */
    public TwoFactorSetupResult setupGoogleAuth(Usuario usuario) {
        // Verificar si ya tiene un secreto configurado
        LocalDateTime now = LocalDateTime.now();
        var existingSecret = tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET, now);
        
        String secret;
        if (existingSecret.isPresent()) {
            secret = existingSecret.get().getCodigo();
        } else {
            // Generar nuevo secreto
            secret = generateGoogleAuthSecret();
            
            // Crear token con el secreto
            Token token = new Token();
            token.setUsuario(usuario);
            token.setCodigo(secret);
            token.setTipoValidacion(TIPO_GOOGLE_AUTH_SECRET);
            token.setFechaExp(LocalDateTime.now().plusYears(10)); // El secreto no expira
            
            tokenRepository.save(token);
        }
        
        // Crear token pendiente para indicar que necesita confirmación
        Token pendingToken = new Token();
        pendingToken.setUsuario(usuario);
        pendingToken.setCodigo("PENDING");
        pendingToken.setTipoValidacion(TIPO_GOOGLE_AUTH_PENDING);
        pendingToken.setFechaExp(LocalDateTime.now().plusDays(7)); // Válido por 7 días
        
        tokenRepository.save(pendingToken);
        
        String qrCodeUrl = generateQRCodeUrl(secret, usuario.getUsuario(), "Helisa");
        return new TwoFactorSetupResult(qrCodeUrl, secret);
    }

    /**
     * Clase para devolver el resultado del setup
     */
    public static class TwoFactorSetupResult {
        private final String qrCodeUrl;
        private final String secret;

        public TwoFactorSetupResult(String qrCodeUrl, String secret) {
            this.qrCodeUrl = qrCodeUrl;
            this.secret = secret;
        }

        public String getQrCodeUrl() { return qrCodeUrl; }
        public String getSecret() { return secret; }
    }

    /**
     * Valida el código de doble autenticación según el tipo configurado
     */
    public boolean validateTwoFactorCode(Usuario usuario, String codigo) {
        if (usuario.getDobleAutenticacion() == null || !usuario.getDobleAutenticacion()) {
            return true; // Si no tiene 2FA habilitado, siempre válido
        }

        // Verificar límite de intentos
        if (hasExceededAttemptLimit(usuario, TIPO_EMAIL_CODE)) {
            throw new RuntimeException("Has excedido el límite de intentos. Intenta más tarde.");
        }

        // Obtener el secreto de Google Auth del usuario
        LocalDateTime now = LocalDateTime.now();
        var googleSecret = tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET, now);
        
        if (googleSecret.isPresent()) {
            // Validar con Google Authenticator
            try {
                int code = Integer.parseInt(codigo);
                return validateGoogleAuthCode(googleSecret.get().getCodigo(), code);
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            // Validar con código de email
            return validateEmailCode(codigo, usuario);
        }
    }

    /**
     * Verifica si el usuario ha excedido el límite de intentos
     * Usa el valor configurable desde properties
     */
    private boolean hasExceededAttemptLimit(Usuario usuario, String tipo) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Long attempts = tokenRepository.countTokensByUsuarioAndTipoSince(usuario, tipo, oneHourAgo);
        return attempts >= maxAttemptsPerHour;
    }

    /**
     * Limpia tokens expirados
     */
    public void cleanExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }

    /**
     * Deshabilita la doble autenticación para un usuario
     */
    public void disableTwoFactorAuth(Usuario usuario) {
        // Eliminar todos los tokens de 2FA del usuario
        var tokens = tokenRepository.findByUsuarioOrderByFechaExpDesc(usuario);
        tokens.stream()
                .filter(token -> TIPO_GOOGLE_AUTH_SECRET.equals(token.getTipoValidacion()) || 
                                TIPO_EMAIL_CODE.equals(token.getTipoValidacion()))
                .forEach(tokenRepository::delete);
        
        // Actualizar el usuario
        usuario.setDobleAutenticacion(false);
    }

    /**
     * Verifica si un usuario tiene Google Authenticator configurado
     */
    public boolean hasGoogleAuthConfigured(String usuario) {
        LocalDateTime now = LocalDateTime.now();
        return tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET, now).isPresent();
    }

    /**
     * Obtiene el secreto existente de Google Authenticator para un usuario
     */
    public String getExistingGoogleAuthSecret(String usuario) {
        LocalDateTime now = LocalDateTime.now();
        return tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET, now)
                .map(Token::getCodigo)
                .orElse(null);
    }

    /**
     * Elimina la configuración de Google Authenticator de un usuario
     */
    public void removeGoogleAuth(String usuario) {
        tokenRepository.deleteByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET);
        tokenRepository.deleteByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_PENDING);
    }

    /**
     * Elimina solo el token pendiente de Google Authenticator
     */
    public void removePendingGoogleAuth(String usuario) {
        tokenRepository.deleteByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_PENDING);
    }

    /**
     * Verifica si un usuario tiene Google Authenticator configurado y confirmado
     */
    public boolean isGoogleAuthConfirmed(String usuario) {
        LocalDateTime now = LocalDateTime.now();
        var googleSecret = tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET, now);
        var pendingToken = tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_PENDING, now);
        
        // Si tiene secreto pero también tiene token pendiente, no está confirmado
        if (googleSecret.isPresent() && pendingToken.isPresent()) {
            return false;
        }
        
        // Si tiene secreto y no tiene token pendiente, está confirmado
        if (googleSecret.isPresent() && !pendingToken.isPresent()) {
            return true;
        }
        
        return false;
    }
}
