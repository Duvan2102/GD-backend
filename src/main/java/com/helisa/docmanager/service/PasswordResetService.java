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

    @Transactional
    public boolean restablecerPassword(String token, String nuevaPassword) {
        TokenInfo tokenInfo = resetTokens.get(token);
        
        if (tokenInfo == null || System.currentTimeMillis() > tokenInfo.getExpirationTime()) {
            resetTokens.remove(token);
            return false;
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(tokenInfo.getUsuarioId());
        if (usuarioOpt.isEmpty()) {
            resetTokens.remove(token);
            return false;
        }

        Usuario usuario = usuarioOpt.get();
        
        if (nuevaPassword == null || nuevaPassword.trim().isEmpty() || nuevaPassword.length() < 6) {
            return false;
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        
        resetTokens.remove(token);
        
        return true;
    }

    @Transactional(readOnly = true)
    public boolean validarToken(String token) {
        TokenInfo tokenInfo = resetTokens.get(token);
        return tokenInfo != null && System.currentTimeMillis() <= tokenInfo.getExpirationTime();
    }

    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorToken(String token) {
        TokenInfo tokenInfo = resetTokens.get(token);
        
        if (tokenInfo == null || System.currentTimeMillis() > tokenInfo.getExpirationTime()) {
            return null;
        }

        return usuarioRepository.findById(tokenInfo.getUsuarioId()).orElse(null);
    }

    @Transactional
    public String generarTokenParaActivacion(Integer idUsuario) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
        
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado con ID: " + idUsuario);
        }

        Usuario usuario = usuarioOpt.get();
        
        String token = generarTokenSeguro();
        long expirationTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        
        resetTokens.put(token, new TokenInfo(usuario.getIdUsuario(), expirationTime));
        
        return token;
    }

    private String generarTokenSeguro() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

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

