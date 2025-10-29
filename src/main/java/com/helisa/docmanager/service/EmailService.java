package com.helisa.docmanager.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Servicio para el envío de correos electrónicos utilizando plantillas HTML.
 * Las plantillas se encuentran en /resources/mailTempo/
 */
@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    @Qualifier("emailTemplateEngine")
    private TemplateEngine templateEngine;

    @Value("${mail.from:noreply@docmanager.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Envía una notificación de nueva solicitud a un aprobador.
     *
     * @param destinatarioEmail    Correo del destinatario
     * @param solicitudId         ID de la solicitud
     * @param nombreSolicitud     Nombre de la solicitud
     * @param nombreSolicitante   Nombre completo del solicitante
     * @param esOrdenSecuencial  Indica si la aprobación es en orden secuencial
     * @param esSiguienteAprobador Indica si es notificación al siguiente aprobador
     */
    public void enviarNotificacionNuevaSolicitud(
            String destinatarioEmail,
            Integer solicitudId,
            String nombreSolicitud,
            String nombreSolicitante,
            Boolean esOrdenSecuencial,
            Boolean esSiguienteAprobador) {

        try {
            // Preparar contexto para la plantilla
            Context context = new Context();
            context.setVariable("destinatarioEmail", destinatarioEmail);
            context.setVariable("solicitudId", solicitudId);
            context.setVariable("nombreSolicitud", nombreSolicitud != null ? nombreSolicitud : "Sin nombre");
            context.setVariable("nombreSolicitante", nombreSolicitante != null ? nombreSolicitante : "Usuario");
            context.setVariable("esOrdenSecuencial", esOrdenSecuencial != null && esOrdenSecuencial);
            context.setVariable("esSiguienteAprobador", esSiguienteAprobador != null && esSiguienteAprobador);
            context.setVariable("baseUrl", baseUrl);

            // Procesar plantilla
            String htmlContent = templateEngine.process("nueva-solicitud", context);

            // Crear y enviar correo
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatarioEmail);
            helper.setSubject("Nueva Solicitud de Aprobación #" + solicitudId);
            helper.setText(htmlContent, true); // true indica que es HTML

            mailSender.send(message);

            log.info("Correo de notificación de nueva solicitud enviado exitosamente a: {}", destinatarioEmail);

        } catch (MessagingException e) {
            log.error("Error al enviar correo de notificación de nueva solicitud a {}: {}", 
                    destinatarioEmail, e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo electrónico", e);
        } catch (Exception e) {
            log.error("Error inesperado al procesar plantilla de correo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al procesar plantilla de correo", e);
        }
    }

    /**
     * Método genérico para enviar correos usando plantillas.
     * Útil para futuras implementaciones de otros tipos de correos.
     *
     * @param destinatarioEmail Correo del destinatario
     * @param asunto            Asunto del correo
     * @param nombrePlantilla   Nombre de la plantilla (sin extensión .html)
     * @param context           Contexto con variables para la plantilla
     */
    public void enviarCorreoTemplate(
            String destinatarioEmail,
            String asunto,
            String nombrePlantilla,
            Context context) {

        try {
            // Procesar plantilla
            String htmlContent = templateEngine.process(nombrePlantilla, context);

            // Crear y enviar correo
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatarioEmail);
            helper.setSubject(asunto);
            helper.setText(htmlContent, true); // true indica que es HTML

            mailSender.send(message);

            log.info("Correo enviado exitosamente a: {} usando plantilla: {}", 
                    destinatarioEmail, nombrePlantilla);

        } catch (MessagingException e) {
            log.error("Error al enviar correo a {} usando plantilla {}: {}", 
                    destinatarioEmail, nombrePlantilla, e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo electrónico", e);
        } catch (Exception e) {
            log.error("Error inesperado al procesar plantilla {}: {}", 
                    nombrePlantilla, e.getMessage(), e);
            throw new RuntimeException("Error al procesar plantilla de correo", e);
        }
    }
}
