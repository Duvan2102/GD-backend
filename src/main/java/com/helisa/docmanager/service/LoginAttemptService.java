package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class LoginAttemptService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    private final ConcurrentMap<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    private static final int MAX_ATTEMPTS_PER_HOUR = 10;

    public static class LoginAttempt {
        private int attempts;
        private LocalDateTime firstAttempt;
        private LocalDateTime lastAttempt;
        private boolean locked;

        public LoginAttempt() {
            this.attempts = 0;
            this.firstAttempt = LocalDateTime.now();
            this.lastAttempt = LocalDateTime.now();
            this.locked = false;
        }

        public void recordAttempt() {
            this.attempts++;
            this.lastAttempt = LocalDateTime.now();
            this.locked = this.attempts >= MAX_ATTEMPTS;
        }

        public void reset() {
            this.attempts = 0;
            this.firstAttempt = LocalDateTime.now();
            this.lastAttempt = LocalDateTime.now();
            this.locked = false;
        }

        public boolean isLocked() {
            if (locked && lastAttempt.isBefore(LocalDateTime.now().minusMinutes(LOCKOUT_DURATION_MINUTES))) {
                reset();
                return false;
            }
            return locked;
        }

        public boolean hasExceededHourlyLimit() {
            return attempts >= MAX_ATTEMPTS_PER_HOUR && 
                   firstAttempt.isAfter(LocalDateTime.now().minusHours(1));
        }

        public int getAttempts() { return attempts; }
        public LocalDateTime getFirstAttempt() { return firstAttempt; }
        public LocalDateTime getLastAttempt() { return lastAttempt; }
    }

    public void recordSuccessfulLogin(String username, String ipAddress) {
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt != null) {
            attempt.reset();
        }
        System.out.println("Login exitoso para usuario: " + username + " desde IP: " + ipAddress);
    }

    public void recordFailedLogin(String username, String ipAddress) {
        LoginAttempt attempt = loginAttempts.computeIfAbsent(username, k -> new LoginAttempt());
        attempt.recordAttempt();
        
        System.out.println("Intento de login fallido para usuario: " + username + 
                          " desde IP: " + ipAddress + 
                          " (Intento #" + attempt.getAttempts() + ")");
        
        if (attempt.getAttempts() == MAX_ATTEMPTS) {
            sendLoginAlert(username, ipAddress);
        }
    }

    public boolean isUserLocked(String username) {
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt == null) {
            return false;
        }
        return attempt.isLocked();
    }

    public boolean hasExceededHourlyLimit(String username) {
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt == null) {
            return false;
        }
        return attempt.hasExceededHourlyLimit();
    }

    public int getRemainingAttempts(String username) {
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt == null) {
            return MAX_ATTEMPTS;
        }
        return Math.max(0, MAX_ATTEMPTS - attempt.getAttempts());
    }

    public long getLockoutTimeRemaining(String username) {
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt == null || !attempt.isLocked()) {
            return 0;
        }
        
        LocalDateTime unlockTime = attempt.getLastAttempt().plusMinutes(LOCKOUT_DURATION_MINUTES);
        LocalDateTime now = LocalDateTime.now();
        
        if (unlockTime.isBefore(now)) {
            return 0;
        }
        
        return java.time.Duration.between(now, unlockTime).toMinutes();
    }

    private void sendLoginAlert(String username, String ipAddress) {
        try {
            Usuario usuario = usuarioRepository.findByUsuario(username).orElse(null);
            if (usuario != null) {
                String email = usuario.getCorreoEmpresarial() != null ? 
                              usuario.getCorreoEmpresarial() : usuario.getCorreoPersonal();
                
                if (email != null && !email.trim().isEmpty()) {
                    emailService.sendLoginAttemptAlert(email, 
                                                      usuario.getNombres() + " " + usuario.getApellidos(), 
                                                      ipAddress);
                }
            }
        } catch (Exception e) {
            System.err.println("Error enviando alerta de login: " + e.getMessage());
        }
    }

    public void cleanOldAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        loginAttempts.entrySet().removeIf(entry -> 
            entry.getValue().getLastAttempt().isBefore(cutoff) && !entry.getValue().isLocked());
    }

    public void unlockUser(String username) {
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt != null) {
            attempt.reset();
            System.out.println("Usuario desbloqueado manualmente: " + username);
        }
    }

    public String getAttemptStats(String username) {
        LoginAttempt attempt = loginAttempts.get(username);
        if (attempt == null) {
            return "No hay intentos registrados";
        }
        
        return String.format("Intentos: %d/%d, Último intento: %s, Bloqueado: %s", 
                           attempt.getAttempts(), 
                           MAX_ATTEMPTS,
                           attempt.getLastAttempt().toString(),
                           attempt.isLocked() ? "Sí" : "No");
    }
}
