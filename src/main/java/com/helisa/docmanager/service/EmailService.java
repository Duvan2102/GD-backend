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
     * Envía correo de activación con token para que el usuario cree su contraseña
     * Usado para usuarios PENDIENTES que están siendo activados por primera vez
     * @param usuario Usuario que fue activado
     * @param token Token de restablecimiento de contraseña
     */
    public void enviarCorreoActivacionConToken(Usuario usuario, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(usuario.getCorreoEmpresarial());
            message.setSubject("Cuenta Activada - Helisa Document Manager");
            
            String body = String.format(
                "Hola %s %s,\n\n" +
                "Su cuenta ha sido activada exitosamente en el sistema Helisa Document Manager.\n\n" +
                "Para completar la configuración de su cuenta, debe establecer una contraseña.\n" +
                "Haga clic en el siguiente enlace para crear su contraseña:\n\n" +
                "%s/reset-password?token=%s\n\n" +
                "Este enlace expirará en 24 horas.\n\n" +
                "Usuario: %s\n\n" +
                "Si no solicitó esta activación, por favor contacte al administrador del sistema.\n\n" +
                "Saludos,\n" +
                "Equipo Helisa",
                usuario.getNombres(),
                usuario.getApellidos(),
                frontendUrl,
                token,
                usuario.getUsuario()
            );
            
            message.setText(body);
            mailSender.send(message);
            
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
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(usuario.getCorreoEmpresarial());
            message.setSubject("Cuenta Reactivada - Helisa Document Manager");
            
            String body = String.format(
                "Hola %s %s,\n\n" +
                "Su cuenta ha sido reactivada exitosamente en el sistema Helisa Document Manager.\n\n" +
                "Ahora puede acceder nuevamente al sistema con sus credenciales anteriores.\n\n" +
                "Usuario: %s\n\n" +
                "Si desea cambiar su contraseña, puede hacerlo desde la opción de restablecer contraseña en la página de inicio de sesión.\n\n" +
                "Si no solicitó esta reactivación, por favor contacte al administrador del sistema inmediatamente.\n\n" +
                "Saludos,\n" +
                "Equipo Helisa",
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getUsuario()
            );
            
            message.setText(body);
            mailSender.send(message);
            
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
     * Envía código de verificación de dos factores por email
     * @param email Email del usuario
     * @param codigo Código de verificación
     * @param nombreCompleto Nombre completo del usuario
     */
    public void sendTwoFactorCode(String email, String codigo, String nombreCompleto) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Código de Verificación - Helisa Document Manager");
            
            String body = String.format(
                "Hola %s,\n\n" +
                "Su código de verificación de dos factores es:\n\n" +
                "%s\n\n" +
                "Este código expirará en 10 minutos.\n\n" +
                "Si no solicitó este código, por favor ignore este correo.\n\n" +
                "Saludos,\n" +
                "Equipo Helisa",
                nombreCompleto,
                codigo
            );
            
            message.setText(body);
            mailSender.send(message);
            
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
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Recordatorio de Solicitud - Helisa Document Manager");

            String body = String.format(
                "recuerda revisar la solicitud N° %d",
                numeroSolicitud
            );

            message.setText(body);
            mailSender.send(message);

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
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Nueva Solicitud Pendiente - Helisa Document Manager");

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

            String body = String.format(
                "Hola,\n\n" +
                "Se ha creado una nueva solicitud que requiere su revisión:\n\n" +
                "• Número de Solicitud: %d\n" +
                "• Nombre: %s\n" +
                "• Solicitante: %s\n" +
                "• Tipo de Aprobación: %s\n\n" +
                "%s\n\n" +
                "Para revisar y gestionar esta solicitud, por favor acceda al sistema:\n" +
                "%s\n\n" +
                "Saludos,\n" +
                "Equipo Helisa Document Manager",
                numeroSolicitud,
                nombreSolicitud != null ? nombreSolicitud : "Sin nombre",
                nombreSolicitante != null ? nombreSolicitante : "Usuario",
                tipoAprobacion,
                instrucciones,
                frontendUrl
            );

            message.setText(body);
            mailSender.send(message);

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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Alerta de Seguridad - Intentos de Login Sospechosos");
        
        String body = String.format(
            "Hola %s,\n\n" +
            "Hemos detectado múltiples intentos de login fallidos en su cuenta.\n\n" +
            "Detalles del intento:\n" +
            "- IP: %s\n" +
            "- Fecha: %s\n\n" +
            "Si no fue usted quien intentó acceder, por favor:\n" +
            "1. Cambie su contraseña inmediatamente\n" +
            "2. Revise la seguridad de su cuenta\n" +
            "3. Contacte al administrador del sistema\n\n" +
            "Saludos,\n" +
            "Equipo de Seguridad Helisa",
            nombreCompleto,
            ipAddress,
            java.time.LocalDateTime.now().toString()
        );
        
        message.setText(body);
        mailSender.send(message);
        
    } catch (Exception e) {
        throw new RuntimeException("Error al enviar alerta de login: " + e.getMessage(), e);
    }
}
}