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

    @Transactional
    public boolean solicitarRestablecimiento(String email, String username) {
        Optional<Usuario> usuarioOpt = Optional.empty();
        
        if (email != null && !email.trim().isEmpty()) {
            usuarioOpt = usuarioRepository.findByCorreoEmpresarial(email);
        }
        
        if (usuarioOpt.isEmpty() && username != null && !username.trim().isEmpty()) {
            usuarioOpt = usuarioRepository.findByUsuario(username);
        }
        
        if (usuarioOpt.isEmpty()) {
            return false;
        }

        Usuario usuario = usuarioOpt.get();
        
        String token = generarTokenSeguro();
        long expirationTime = System.currentTimeMillis() + (60 * 60 * 1000);
        
        resetTokens.put(token, new TokenInfo(usuario.getIdUsuario(), expirationTime));
        
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
     * Genera un token para activación de usuario pendiente
     * Similar a solicitarRestablecimiento pero usando el ID del usuario
     * Usado cuando se activa un usuario pendiente para que cree su contraseña
     * 
     * @param idUsuario ID del usuario
     * @return Token generado
     */
    @Transactional
    public String generarTokenParaActivacion(Integer idUsuario) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
        
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado con ID: " + idUsuario);
        }

        Usuario usuario = usuarioOpt.get();
        
        // Generar token de restablecimiento con expiración de 24 horas para activación
        String token = generarTokenSeguro();
        long expirationTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 horas
        
        // Guardar token
        resetTokens.put(token, new TokenInfo(usuario.getIdUsuario(), expirationTime));
        
        return token;
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

