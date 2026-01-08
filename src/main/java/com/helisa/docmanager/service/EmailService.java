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

    private void enviarCorreoHtml(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            
            String htmlContent = templateEngine.process("mailTemp/" + templateName, context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar correo: " + e.getMessage(), e);
        }
    }

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

 
    @Deprecated
    public void enviarCorreoActivacion(Usuario usuario) {
        enviarCorreoActivacionConToken(usuario, generarTokenRestablecimiento(usuario));
    }


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


    private String generarTokenRestablecimiento(Usuario usuario) {
        long timestamp = System.currentTimeMillis();
        return String.format("%d_%d", usuario.getIdUsuario(), timestamp);
    }


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
