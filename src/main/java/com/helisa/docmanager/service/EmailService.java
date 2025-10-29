package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username:noreply@helisa.com}")
    private String fromEmail;

    /**
     * Método auxiliar para enviar correos HTML usando plantillas Thymeleaf
     * @param to Email del destinatario
     * @param subject Asunto del correo
     * @param templateName Nombre de la plantilla (sin extensión .html)
     * @param context Contexto con las variables para la plantilla
     */
    private void enviarCorreoHtml(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            
            String htmlContent = templateEngine.process("mailTempo/" + templateName, context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

    /**
     * Envía correo de activación con token para que el usuario cree su contraseña
     * Usado para usuarios PENDIENTES que están siendo activados por primera vez
     * @param usuario Usuario que fue activado
     * @param token Token de restablecimiento de contraseña
     */
    public void enviarCorreoActivacionConToken(Usuario usuario, String token) {
        try {
            String nombreCompleto = Stream.of(usuario.getNombres(), usuario.getApellidos())
                    .filter(java.util.Objects::nonNull)
                    .map(String::trim)
                    .filter(str -> !str.isEmpty())
                    .collect(Collectors.joining(" "));
            
            String resetPasswordUrl = frontendUrl + "/reset-password?token=" + token;
            
            Context context = new Context();
            context.setVariable("nombreCompleto", nombreCompleto.isEmpty() ? "Usuario" : nombreCompleto);
            context.setVariable("usuario", usuario.getUsuario());
            context.setVariable("resetPasswordUrl", resetPasswordUrl);
            
            enviarCorreoHtml(
                    usuario.getCorreoEmpresarial(),
                    "Cuenta Activada - Helisa Document Manager",
                    "activacion-con-token",
                    context
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar correo de activación: " + e.getMessage(), e);
        }
    }

    /**
     * Envía correo de reactivación para usuarios INACTIVOS
     * @param usuario Usuario que fue reactivado
     */
    public void enviarCorreoReactivacion(Usuario usuario) {
        try {
            String nombreCompleto = Stream.of(usuario.getNombres(), usuario.getApellidos())
                    .filter(java.util.Objects::nonNull)
                    .map(String::trim)
                    .filter(str -> !str.isEmpty())
                    .collect(Collectors.joining(" "));
            
            Context context = new Context();
            context.setVariable("nombreCompleto", nombreCompleto.isEmpty() ? "Usuario" : nombreCompleto);
            context.setVariable("usuario", usuario.getUsuario());
            context.setVariable("frontendUrl", frontendUrl);
            
            enviarCorreoHtml(
                    usuario.getCorreoEmpresarial(),
                    "Cuenta Reactivada - Helisa Document Manager",
                    "reactivacion",
                    context
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar correo de reactivación: " + e.getMessage(), e);
        }
    }

    /**
     * @deprecated Usar enviarCorreoActivacionConToken en su lugar
     * Envía correo de activación con URL para restablecer contraseña
     * @param usuario Usuario que fue activado
     */
    @Deprecated
    public void enviarCorreoActivacion(Usuario usuario) {
        // Mantener compatibilidad pero redirigir al método nuevo
        enviarCorreoActivacionConToken(usuario, generarTokenRestablecimiento(usuario));
    }

    /**
     * Envía correo de restablecimiento de contraseña
     * @param usuario Usuario que solicita restablecimiento
     * @param token Token de restablecimiento
     */
    public void enviarCorreoRestablecimiento(Usuario usuario, String token) {
        try {
            String nombreCompleto = Stream.of(usuario.getNombres(), usuario.getApellidos())
                    .filter(java.util.Objects::nonNull)
                    .map(String::trim)
                    .filter(str -> !str.isEmpty())
                    .collect(Collectors.joining(" "));
            
            String resetPasswordUrl = frontendUrl + "/reset-password?token=" + token;
            
            Context context = new Context();
            context.setVariable("nombreCompleto", nombreCompleto.isEmpty() ? "Usuario" : nombreCompleto);
            context.setVariable("resetPasswordUrl", resetPasswordUrl);
            
            enviarCorreoHtml(
                    usuario.getCorreoEmpresarial(),
                    "Restablecer Contraseña - Helisa Document Manager",
                    "restablecimiento-password",
                    context
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar correo de restablecimiento: " + e.getMessage(), e);
        }
    }

    /**
     * Envía código de verificación de dos factores por email
     * @param email Email del usuario
     * @param codigo Código de verificación
     * @param nombreCompleto Nombre completo del usuario
     */
    public void sendTwoFactorCode(String email, String codigo, String nombreCompleto) {
        try {
            Context context = new Context();
            context.setVariable("nombreCompleto", nombreCompleto != null && !nombreCompleto.trim().isEmpty() 
                    ? nombreCompleto : "Usuario");
            context.setVariable("codigo", codigo);
            
            enviarCorreoHtml(
                    email,
                    "Código de Verificación - Helisa Document Manager",
                    "codigo-2fa",
                    context
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar código de verificación: " + e.getMessage(), e);
        }
    }

    /**
     * Envía correo simple de recordatorio de solicitud
     * @param email Email del destinatario
     * @param numeroSolicitud Número/ID de la solicitud
     */
    public void enviarRecordatorioSolicitud(String email, Integer numeroSolicitud) {
        try {
            Context context = new Context();
            context.setVariable("numeroSolicitud", numeroSolicitud);
            context.setVariable("frontendUrl", frontendUrl);
            
            enviarCorreoHtml(
                    email,
                    "Recordatorio de Solicitud - Helisa Document Manager",
                    "recordatorio-solicitud",
                    context
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar recordatorio de solicitud: " + e.getMessage(), e);
        }
    }

    /**
     * Envía notificación de nueva solicitud creada a los aprobadores
     * @param email Email del destinatario
     * @param numeroSolicitud Número/ID de la solicitud
     * @param nombreSolicitud Nombre de la solicitud
     * @param nombreSolicitante Nombre del solicitante
     * @param esOrdenSecuencial Si la solicitud requiere orden secuencial
     * @param esSiguienteAprobador Si este usuario es el siguiente en aprobar (solo aplica si es orden secuencial)
     */
    public void enviarNotificacionNuevaSolicitud(String email, Integer numeroSolicitud, 
                                                String nombreSolicitud, String nombreSolicitante,
                                                boolean esOrdenSecuencial, boolean esSiguienteAprobador) {
        try {
            String tipoAprobacion;
            String instrucciones;
            
            if (esOrdenSecuencial) {
                if (esSiguienteAprobador) {
                    tipoAprobacion = "APROBACIÓN SECUENCIAL - Es su turno";
                    instrucciones = "Esta solicitud requiere aprobación en orden secuencial. Usted es el siguiente en la cola de aprobación.";
                } else {
                    tipoAprobacion = "APROBACIÓN SECUENCIAL - En espera";
                    instrucciones = "Esta solicitud requiere aprobación en orden secuencial. Su turno llegará cuando los aprobadores anteriores hayan completado su revisión.";
                }
            } else {
                tipoAprobacion = "APROBACIÓN SIMULTÁNEA";
                instrucciones = "Esta solicitud puede ser aprobada por todos los destinatarios simultáneamente.";
            }
            
            Context context = new Context();
            context.setVariable("numeroSolicitud", numeroSolicitud);
            context.setVariable("nombreSolicitud", nombreSolicitud != null ? nombreSolicitud : "Sin nombre");
            context.setVariable("nombreSolicitante", nombreSolicitante != null ? nombreSolicitante : "Usuario");
            context.setVariable("tipoAprobacion", tipoAprobacion);
            context.setVariable("instrucciones", instrucciones);
            context.setVariable("esOrdenSecuencial", esOrdenSecuencial);
            context.setVariable("esSiguienteAprobador", esSiguienteAprobador);
            context.setVariable("frontendUrl", frontendUrl);
            
            enviarCorreoHtml(
                    email,
                    "Nueva Solicitud Pendiente - Helisa Document Manager",
                    "nueva-solicitud",
                    context
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar notificación de nueva solicitud: " + e.getMessage(), e);
        }
    }

    /**
     * Genera un token simple para restablecimiento de contraseña
     * En un entorno de producción, esto debería ser más seguro
     * @param usuario Usuario para el cual generar el token
     * @return Token generado
     */
    private String generarTokenRestablecimiento(Usuario usuario) {
        // Generar un token simple basado en el ID del usuario y timestamp
        long timestamp = System.currentTimeMillis();
        return String.format("%d_%d", usuario.getIdUsuario(), timestamp);
    }

    /**
     * Envía alerta de intentos de login sospechosos
     * @param email Email del usuario
     * @param nombreCompleto Nombre completo del usuario
     * @param ipAddress Dirección IP desde donde se intentó el login
     */
    public void sendLoginAttemptAlert(String email, String nombreCompleto, String ipAddress) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String fechaIntento = LocalDateTime.now().format(formatter);
            
            Context context = new Context();
            context.setVariable("nombreCompleto", nombreCompleto != null && !nombreCompleto.trim().isEmpty() 
                    ? nombreCompleto : "Usuario");
            context.setVariable("ipAddress", ipAddress != null ? ipAddress : "No disponible");
            context.setVariable("fechaIntento", fechaIntento);
            context.setVariable("frontendUrl", frontendUrl);
            
            enviarCorreoHtml(
                    email,
                    "Alerta de Seguridad - Intentos de Login Sospechosos",
                    "alerta-login",
                    context
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar alerta de login: " + e.getMessage(), e);
        }
    }
}
