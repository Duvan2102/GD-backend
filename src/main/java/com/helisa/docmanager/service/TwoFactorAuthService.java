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

    public static final String TIPO_GOOGLE_AUTH = "GOOGLE_AUTH";
    public static final String TIPO_EMAIL_CODE = "EMAIL_CODE";
    public static final String TIPO_GOOGLE_AUTH_SECRET = "GOOGLE_AUTH_SECRET";
    public static final String TIPO_GOOGLE_AUTH_PENDING = "GOOGLE_AUTH_PENDING";

    @Value("${app.2fa.email-code.validity-minutes:5}")
    private int emailCodeValidityMinutes;
    
    @Value("${app.2fa.max-attempts-per-hour:5}")
    private int maxAttemptsPerHour;

    public String generateEmailCode() {
        return String.format("%06d", random.nextInt(1000000));
    }

    public String generateGoogleAuthSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    public String generateQRCodeUrl(String secret, String usuario, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", 
                           issuer, usuario, secret, issuer);
    }

    public boolean validateGoogleAuthCode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }

    @Transactional
    public boolean validateEmailCode(String codigo, Usuario usuario) {
        LocalDateTime now = LocalDateTime.now();
        
        var tokenOpt = tokenRepository.findValidTokenByCodigoUsuarioAndTipo(codigo, usuario, TIPO_EMAIL_CODE, now);
        
        if (tokenOpt.isEmpty()) {
            return false;
        }
        
        Token token = tokenOpt.get();
        
        var tokensUsuario = tokenRepository.findValidTokensByUsuarioAndTipoOrderByFechaExpDesc(
            usuario, TIPO_EMAIL_CODE, now);
        
        if (!tokensUsuario.isEmpty() && !tokensUsuario.get(0).getIdToken().equals(token.getIdToken())) {
            return false;
        }
        
        tokenRepository.delete(token);
        
        return true;
    }


    @Transactional
    public Token sendEmailCode(Usuario usuario) {
        if (hasExceededAttemptLimit(usuario, TIPO_EMAIL_CODE)) {
            throw new RuntimeException("Has excedido el límite de intentos. Intenta más tarde.");
        }

        LocalDateTime now = LocalDateTime.now();
        
        List<Token> tokensPrevios = tokenRepository.findValidTokensByUsuarioAndTipoOrderByFechaExpDesc(
            usuario, TIPO_EMAIL_CODE, now);
        
        if (!tokensPrevios.isEmpty()) {
            Token tokenActual = tokensPrevios.get(0);
            long minutosRestantes = java.time.Duration.between(now, tokenActual.getFechaExp()).toMinutes();
            throw new RuntimeException("Ya existe un código válido enviado. Expira en " + minutosRestantes + " minuto(s). Usa ese código o espera a que expire.");
        }

        String codigo = generateEmailCode();
        
        Token token = new Token();
        token.setUsuario(usuario);
        token.setCodigo(codigo);
        token.setTipoValidacion(TIPO_EMAIL_CODE);
        token.setFechaExp(LocalDateTime.now().plusMinutes(emailCodeValidityMinutes));
        
        token = tokenRepository.save(token);
        
        String email = usuario.getCorreoEmpresarial() != null ? 
                      usuario.getCorreoEmpresarial() : usuario.getCorreoPersonal();
        
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("El usuario no tiene un email configurado");
        }
        
        emailService.sendTwoFactorCode(email, codigo, usuario.getNombres() + " " + usuario.getApellidos());
        
        return token;
    }

    public TwoFactorSetupResult setupGoogleAuth(Usuario usuario) {
        LocalDateTime now = LocalDateTime.now();
        var existingSecret = tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET, now);
        
        String secret;
        if (existingSecret.isPresent()) {
            secret = existingSecret.get().getCodigo();
        } else {
            secret = generateGoogleAuthSecret();
            
            Token token = new Token();
            token.setUsuario(usuario);
            token.setCodigo(secret);
            token.setTipoValidacion(TIPO_GOOGLE_AUTH_SECRET);
            token.setFechaExp(LocalDateTime.now().plusYears(10));
            
            tokenRepository.save(token);
        }
        
        String qrCodeUrl = generateQRCodeUrl(secret, usuario.getUsuario(), "Helisa");
        return new TwoFactorSetupResult(qrCodeUrl, secret);
    }

    public void createPendingGoogleAuthToken(Usuario usuario) {
        LocalDateTime now = LocalDateTime.now();
        var existingPending = tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_PENDING, now);
        
        if (existingPending.isPresent()) {
            return;
        }
        
        Token pendingToken = new Token();
        pendingToken.setUsuario(usuario);
        pendingToken.setCodigo("PENDING");
        pendingToken.setTipoValidacion(TIPO_GOOGLE_AUTH_PENDING);
        pendingToken.setFechaExp(LocalDateTime.now().plusDays(7));
        
        tokenRepository.save(pendingToken);
    }

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

    public boolean validateTwoFactorCode(Usuario usuario, String codigo) {
        if (hasExceededAttemptLimit(usuario, TIPO_EMAIL_CODE)) {
            throw new RuntimeException("Has excedido el límite de intentos. Intenta más tarde.");
        }

        if (usuario.getTokenQr() != null && usuario.getTokenQr()) {
            return validarCodigoGoogleAuth(usuario, codigo);
        } else if (usuario.getTokenCorreo() != null && usuario.getTokenCorreo()) {
            return validarCodigoEmail(usuario, codigo);
        } else {
            throw new RuntimeException("Usuario no tiene método de 2FA configurado");
        }
    }

    public boolean validarCodigoGoogleAuth(Usuario usuario, String codigo) {
        LocalDateTime now = LocalDateTime.now();
        var googleSecret = tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET, now);
        
        if (!googleSecret.isPresent()) {
            throw new RuntimeException("Google Authenticator no está configurado para este usuario");
        }
        
        try {
            int code = Integer.parseInt(codigo);
            return validateGoogleAuthCode(googleSecret.get().getCodigo(), code);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean validarCodigoEmail(Usuario usuario, String codigo) {
        return validateEmailCode(codigo, usuario);
    }

    private boolean hasExceededAttemptLimit(Usuario usuario, String tipo) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Long attempts = tokenRepository.countTokensByUsuarioAndTipoSince(usuario, tipo, oneHourAgo);
        return attempts >= maxAttemptsPerHour;
    }

    public void cleanExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }

    public void disableTwoFactorAuth(Usuario usuario) {
        var tokens = tokenRepository.findByUsuarioOrderByFechaExpDesc(usuario);
        tokens.stream()
                .filter(token -> TIPO_GOOGLE_AUTH_SECRET.equals(token.getTipoValidacion()) || 
                                TIPO_EMAIL_CODE.equals(token.getTipoValidacion()))
                .forEach(tokenRepository::delete);
        
    }

    public boolean hasGoogleAuthConfigured(String usuario) {
        LocalDateTime now = LocalDateTime.now();
        return tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET, now).isPresent();
    }

    public boolean hasAnyGoogleAuthToken(String usuario) {
        LocalDateTime now = LocalDateTime.now();
        boolean hasSecret = tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET, now).isPresent();
        boolean hasPending = tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_PENDING, now).isPresent();
        return hasSecret || hasPending;
    }

    public String getExistingGoogleAuthSecret(String usuario) {
        LocalDateTime now = LocalDateTime.now();
        return tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET, now)
                .map(Token::getCodigo)
                .orElse(null);
    }

    public void removeGoogleAuth(String usuario) {
        tokenRepository.deleteByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET);
        tokenRepository.deleteByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_PENDING);
    }

    public void removePendingGoogleAuth(String usuario) {
        tokenRepository.deleteByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_PENDING);
    }

    public boolean isGoogleAuthConfirmed(String usuario) {
        LocalDateTime now = LocalDateTime.now();
        var googleSecret = tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_SECRET, now);
        var pendingToken = tokenRepository.findValidTokenByUsuarioAndTipo(usuario, TIPO_GOOGLE_AUTH_PENDING, now);
        
        if (googleSecret.isPresent() && pendingToken.isPresent()) {
            return false;
        }
        
        if (googleSecret.isPresent() && !pendingToken.isPresent()) {
            return true;
        }
        
        return false;
    }

    public void activarMetodoEmail(Usuario usuario) {
        usuario.setTokenCorreo(true);
        usuario.setTokenQr(false);
        
        tokenRepository.deleteByUsuarioAndTipo(usuario.getUsuario(), TIPO_GOOGLE_AUTH_SECRET);
        tokenRepository.deleteByUsuarioAndTipo(usuario.getUsuario(), TIPO_GOOGLE_AUTH_PENDING);
    }

    public void activarMetodoGoogleAuth(Usuario usuario) {
        usuario.setTokenQr(true);
        usuario.setTokenCorreo(false);
        
        var tokensEmail = tokenRepository.findValidTokensByUsuarioAndTipoOrderByFechaExpDesc(
            usuario, TIPO_EMAIL_CODE, LocalDateTime.now());
        tokensEmail.forEach(tokenRepository::delete);
    }
}
