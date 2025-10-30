package com.helisa.docmanager.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${security.jwt.secret:please-change-this-secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms}") 
    private long expirationMs;

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        return JWT.create()
                .withSubject(userDetails.getUsername())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusMillis(expirationMs)))
                .sign(algorithm);
    }

    public String extractUsername(String token) {
        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token);
        return decoded.getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT decoded = JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token);
            return decoded.getExpiresAt().before(new Date());
        } catch (Exception e) {
            return true; // Si no se puede verificar, consideramos que está expirado
        }
    }

    public String extractJti(String token) {
        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token);
        return decoded.getId();
    }

    public Instant extractExpiration(String token) {
        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token);
        return decoded.getExpiresAt().toInstant();
    }

    /**
     * Genera un token temporal para validación de 2FA (válido por 5 minutos)
     */
    public String generateTempToken(String username) {
        Instant now = Instant.now();
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        return JWT.create()
                .withSubject(username)
                .withClaim("type", "temp_2fa")
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusMillis(300000))) // 5 minutos
                .sign(algorithm);
    }

    /**
     * Valida si un token temporal es válido
     */
    public boolean isTempTokenValid(String token) {
        try {
            DecodedJWT decoded = JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token);
            return "temp_2fa".equals(decoded.getClaim("type").asString()) && 
                   decoded.getExpiresAt().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extrae el username de un token temporal
     */
    public String extractUsernameFromTempToken(String token) {
        try {
            DecodedJWT decoded = JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token);
            return decoded.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Token temporal inválido", e);
        }
    }
}
