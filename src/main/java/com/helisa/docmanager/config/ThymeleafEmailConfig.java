package com.helisa.docmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Configuración de Thymeleaf específica para plantillas de correo electrónico.
 * Esta configuración permite usar Thymeleaf para procesar las plantillas HTML
 * que se encuentran en /resources/mailTempo/
 */
@Configuration
public class ThymeleafEmailConfig {

    /**
     * Template resolver para las plantillas de correo.
     * Busca las plantillas en classpath:/mailTempo/
     */
    @Bean(name = "emailTemplateResolver")
    public SpringResourceTemplateResolver emailTemplateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setPrefix("classpath:/mailTempo/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false); // Desactivar cache en desarrollo
        templateResolver.setOrder(1);
        return templateResolver;
    }

    /**
     * Template engine específico para correos electrónicos.
     * Utiliza el template resolver configurado para email.
     */
    @Bean(name = "emailTemplateEngine")
    public SpringTemplateEngine emailTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(emailTemplateResolver());
        return templateEngine;
    }
}
