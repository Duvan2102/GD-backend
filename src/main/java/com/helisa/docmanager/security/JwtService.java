package com.helisa.docmanager.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
// import java.util.UUID; // Comentado - no se usa actualmente

@Service
public class JwtService {

    @Value("${security.jwt.secret:please-change-this-secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms:86400000}") // 1 día
    private long expirationMs;

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        return JWT.create()
                .withSubject(userDetails.getUsername())
                // .withJWTId(UUID.randomUUID().toString()) // Comentado - no esencial para CORS
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusMillis(expirationMs)))
                .sign(algorithm);
    }

    public String extractUsername(String token) {
        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token);
        return decoded.getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername());
    }

    // Métodos comentados para simplificar - no esenciales para CORS
    // public String extractJti(String token) {
    //     DecodedJWT decoded = JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token);
    //     return decoded.getId();
    // }

    // public Instant extractExpiration(String token) {
    //     DecodedJWT decoded = JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token);
    //     return decoded.getExpiresAt().toInstant();
    // }
}
