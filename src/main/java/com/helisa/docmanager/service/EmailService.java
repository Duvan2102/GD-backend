package com.helisa.docmanager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@helisa.com}")
    private String fromEmail;

    @Value("${app.mail.subject.prefix:[Helisa]}")
    private String subjectPrefix;

    public void sendTwoFactorCode(String toEmail, String codigo, String nombreUsuario) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subjectPrefix + " Código de Verificación");
            
            String body = String.format("""
                Hola %s,
                
                Has solicitado un código de verificación para acceder a tu cuenta.
                
                Tu código de verificación es: %s
                
                Este código expirará en 10 minutos.
                
                Si no solicitaste este código, por favor ignora este mensaje.
                
                Saludos,
                Equipo Helisa
                """, nombreUsuario, codigo);
            
            message.setText(body);
            
            mailSender.send(message);
            System.out.println("Email enviado exitosamente a: " + toEmail);
        } catch (Exception e) {
            System.err.println("Error enviando email: " + e.getMessage());
            throw new RuntimeException("Error enviando código de verificación", e);
        }
    }

    public void sendLoginAttemptAlert(String toEmail, String nombreUsuario, String ipAddress) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subjectPrefix + " Alerta de Intento de Inicio de Sesión");
            
            String body = String.format("""
                Hola %s,
                
                Se ha detectado un intento de inicio de sesión en tu cuenta desde la IP: %s
                
                Si fuiste tú, puedes ignorar este mensaje.
                Si no fuiste tú, por favor cambia tu contraseña inmediatamente.
                
                Saludos,
                Equipo Helisa
                """, nombreUsuario, ipAddress);
            
            message.setText(body);
            
            mailSender.send(message);
            System.out.println("Alerta de intento de login enviada a: " + toEmail);
        } catch (Exception e) {
            System.err.println("Error enviando alerta de login: " + e.getMessage());
        }
    }
}
