package com.helisa.docmanager.service;

import com.helisa.docmanager.repository.TokenRepository;
import com.helisa.docmanager.repository.RevokedTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class TokenCleanupService {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Scheduled(fixedRate = 600000) // 10 minutos
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = tokenRepository.deleteExpiredTokens(now);
            if (deletedCount > 0) {
                log.info("TokenCleanupService - Eliminados {} tokens expirados", deletedCount);
            }
        } catch (Exception e) {
            log.error("TokenCleanupService - Error al limpiar tokens expirados: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3600000) // 1 hora
    @Transactional
    public void cleanupRevokedTokens() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);
            long deletedCount = revokedTokenRepository.deleteByExpiresAtBefore(cutoffDate);
            if (deletedCount > 0) {
                log.info("TokenCleanupService - Eliminados {} tokens revocados expirados", deletedCount);
            }
        } catch (Exception e) {
            log.error("TokenCleanupService - Error al limpiar tokens revocados: {}", e.getMessage());
        }
    }
}