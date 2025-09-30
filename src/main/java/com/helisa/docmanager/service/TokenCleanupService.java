package com.helisa.docmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Servicio para limpieza automática de tokens expirados
 * Evita saturar la base de datos con códigos de 2FA viejos
 */
@Service
public class TokenCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupService.class);

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    @Value("${app.2fa.email-code.validity-minutes:5}")
    private int emailCodeValidityMinutes;

    /**
     * Tarea programada para limpiar tokens expirados
     * Se ejecuta según el intervalo configurado en properties (por defecto cada 10 minutos)
     * 
     * La expresión cron se lee así:
     * 0 - segundo (0)
     * 0 - minuto (0)
     * 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22 - cada 2 horas
     * * - cualquier día del mes
     * * - cualquier mes
     * ? - cualquier día de la semana
     */
    @Scheduled(cron = "${app.2fa.cleanup-cron:0 */10 * * * ?}")
    public void cleanupExpiredTokens() {
        try {
            logger.info("Iniciando limpieza automática de tokens expirados de 2FA...");
            
            twoFactorAuthService.cleanExpiredTokens();
            
            logger.info("Limpieza de tokens expirados completada exitosamente");
        } catch (Exception e) {
            logger.error("Error al limpiar tokens expirados: {}", e.getMessage(), e);
        }
    }

    /**
     * Tarea programada adicional para limpiar tokens periódicamente
     * Esto asegura que los códigos de email (válidos 5 min) se eliminen rápidamente
     * Se ejecuta con un delay fijo en milisegundos (por defecto 10 min = 600000 ms)
     */
    @Scheduled(fixedDelayString = "${app.2fa.cleanup-interval-ms:600000}", initialDelay = 60000)
    public void cleanupExpiredTokensPeriodically() {
        try {
            logger.debug("Ejecutando limpieza periódica de tokens expirados...");
            
            twoFactorAuthService.cleanExpiredTokens();
            
            logger.debug("Limpieza periódica completada");
        } catch (Exception e) {
            logger.error("Error en limpieza periódica de tokens: {}", e.getMessage(), e);
        }
    }
}

