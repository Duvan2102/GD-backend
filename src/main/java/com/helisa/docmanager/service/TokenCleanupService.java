package com.helisa.docmanager.service;

import com.helisa.docmanager.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TokenCleanupService {

    @Autowired
    private TokenRepository tokenRepository;

    /**
     * Limpia tokens expirados cada 10 minutos
     */
    @Scheduled(fixedRate = 600000) // 10 minutos
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = tokenRepository.deleteExpiredTokens(now);
            if (deletedCount > 0) {
                System.out.println("TokenCleanupService - Eliminados " + deletedCount + " tokens expirados");
            }
        } catch (Exception e) {
            System.out.println("TokenCleanupService - Error al limpiar tokens expirados: " + e.getMessage());
        }
    }
}