package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Usuario;
import com.helisa.docmanager.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // En un entorno de producción, esto debería ser una base de datos o cache distribuido
    private final Map<String, TokenInfo> resetTokens = new HashMap<>();

    /**
     * Solicita restablecimiento de contraseña
     * @param email Correo electrónico del usuario
     * @return true si se envió el correo, false si el usuario no existe
     */
    @Transactional
    public boolean solicitarRestablecimiento(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoEmpresarial(email);
        
        if (usuarioOpt.isEmpty()) {
            return false; // No revelar si el usuario existe o no
        }

        Usuario usuario = usuarioOpt.get();
        
        // Generar token de restablecimiento
        String token = generarTokenSeguro();
        long expirationTime = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hora
        
        // Guardar token (en producción, usar base de datos)
        resetTokens.put(token, new TokenInfo(usuario.getIdUsuario(), expirationTime));
        
        // Enviar correo
        emailService.enviarCorreoRestablecimiento(usuario, token);
        
        return true;
    }

    /**
     * Restablece la contraseña usando un token
     * @param token Token de restablecimiento
     * @param nuevaPassword Nueva contraseña
     * @return true si se restableció exitosamente, false si el token es inválido
     */
    @Transactional
    public boolean restablecerPassword(String token, String nuevaPassword) {
        TokenInfo tokenInfo = resetTokens.get(token);
        
        if (tokenInfo == null || System.currentTimeMillis() > tokenInfo.getExpirationTime()) {
            // Token no existe o expirado
            resetTokens.remove(token);
            return false;
        }

        // Buscar usuario
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(tokenInfo.getUsuarioId());
        if (usuarioOpt.isEmpty()) {
            resetTokens.remove(token);
            return false;
        }

        Usuario usuario = usuarioOpt.get();
        
        // Validar nueva contraseña
        if (nuevaPassword == null || nuevaPassword.trim().isEmpty() || nuevaPassword.length() < 6) {
            return false;
        }

        // Actualizar contraseña
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        
        // Eliminar token usado
        resetTokens.remove(token);
        
        return true;
    }

    /**
     * Valida si un token es válido
     * @param token Token a validar
     * @return true si es válido, false en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean validarToken(String token) {
        TokenInfo tokenInfo = resetTokens.get(token);
        return tokenInfo != null && System.currentTimeMillis() <= tokenInfo.getExpirationTime();
    }

    /**
     * Obtiene información del usuario asociado a un token
     * @param token Token de restablecimiento
     * @return Usuario asociado al token, o null si el token es inválido
     */
    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorToken(String token) {
        TokenInfo tokenInfo = resetTokens.get(token);
        
        if (tokenInfo == null || System.currentTimeMillis() > tokenInfo.getExpirationTime()) {
            return null;
        }

        return usuarioRepository.findById(tokenInfo.getUsuarioId()).orElse(null);
    }

    /**
     * Genera un token seguro para restablecimiento
     * @return Token generado
     */
    private String generarTokenSeguro() {
        // En producción, usar un generador de tokens más seguro
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Clase interna para almacenar información del token
     */
    private static class TokenInfo {
        private final Integer usuarioId;
        private final long expirationTime;

        public TokenInfo(Integer usuarioId, long expirationTime) {
            this.usuarioId = usuarioId;
            this.expirationTime = expirationTime;
        }

        public Integer getUsuarioId() {
            return usuarioId;
        }

        public long getExpirationTime() {
            return expirationTime;
        }
    }
}

