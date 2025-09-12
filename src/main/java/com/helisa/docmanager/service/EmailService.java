package com.helisa.docmanager.service;

import com.helisa.docmanager.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username:noreply@helisa.com}")
    private String fromEmail;

    /**
     * Envía correo de activación con URL para restablecer contraseña
     * @param usuario Usuario que fue activado
     */
    public void enviarCorreoActivacion(Usuario usuario) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(usuario.getCorreoEmpresarial());
            message.setSubject("Cuenta Activada - Helisa Document Manager");
            
            String body = String.format(
                "Hola %s %s,\n\n" +
                "Su cuenta ha sido activada exitosamente en el sistema Helisa Document Manager.\n\n" +
                "Para completar la configuración de su cuenta, debe establecer una nueva contraseña.\n" +
                "Haga clic en el siguiente enlace para restablecer su contraseña:\n\n" +
                "%s/reset-password?token=%s\n\n" +
                "Este enlace expirará en 24 horas.\n\n" +
                "Si no solicitó esta activación, por favor ignore este correo.\n\n" +
                "Saludos,\n" +
                "Equipo Helisa",
                usuario.getNombres(),
                usuario.getApellidos(),
                frontendUrl,
                generarTokenRestablecimiento(usuario)
            );
            
            message.setText(body);
            mailSender.send(message);
            
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar correo de activación: " + e.getMessage(), e);
        }
    }

    /**
     * Envía correo de restablecimiento de contraseña
     * @param usuario Usuario que solicita restablecimiento
     * @param token Token de restablecimiento
     */
    public void enviarCorreoRestablecimiento(Usuario usuario, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(usuario.getCorreoEmpresarial());
            message.setSubject("Restablecer Contraseña - Helisa Document Manager");
            
            String body = String.format(
                "Hola %s %s,\n\n" +
                "Ha solicitado restablecer su contraseña en el sistema Helisa Document Manager.\n\n" +
                "Haga clic en el siguiente enlace para establecer una nueva contraseña:\n\n" +
                "%s/reset-password?token=%s\n\n" +
                "Este enlace expirará en 1 hora.\n\n" +
                "Si no solicitó este restablecimiento, por favor ignore este correo.\n\n" +
                "Saludos,\n" +
                "Equipo Helisa",
                usuario.getNombres(),
                usuario.getApellidos(),
                frontendUrl,
                token
            );
            
            message.setText(body);
            mailSender.send(message);
            
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar correo de restablecimiento: " + e.getMessage(), e);
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
}