package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Token;
import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.repository.TokenRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    // Duración de validez de tokens (en minutos)
    private static final int TOKEN_VALIDITY_MINUTES = 10;
    private static final int MAX_ATTEMPTS_PER_HOUR = 5;

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
     */
    public boolean validateEmailCode(String codigo, Usuario usuario) {
        LocalDateTime now = LocalDateTime.now();
        return tokenRepository.findValidTokenByCodigoAndTipo(codigo, TIPO_EMAIL_CODE, now)
                .map(token -> token.getUsuario().getIdUsuario().equals(usuario.getIdUsuario()))
                .orElse(false);
    }

    /**
     * Envía código de verificación por email
     */
    public Token sendEmailCode(Usuario usuario) {
        // Verificar límite de intentos
        if (hasExceededAttemptLimit(usuario, TIPO_EMAIL_CODE)) {
            throw new RuntimeException("Has excedido el límite de intentos. Intenta más tarde.");
        }

        // Generar código
        String codigo = generateEmailCode();
        
        // Crear token
        Token token = new Token();
        token.setUsuario(usuario);
        token.setCodigo(codigo);
        token.setTipoValidacion(TIPO_EMAIL_CODE);
        token.setFechaExp(LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES));
        
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
     */
    private boolean hasExceededAttemptLimit(Usuario usuario, String tipo) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Long attempts = tokenRepository.countTokensByUsuarioAndTipoSince(usuario, tipo, oneHourAgo);
        return attempts >= MAX_ATTEMPTS_PER_HOUR;
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
    }
}
