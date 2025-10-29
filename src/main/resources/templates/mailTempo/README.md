# Plantillas de Correo Electrónico - Helisa Document Manager

Este documento enumera todos los tipos de correos electrónicos que se envían desde el sistema y sus respectivas plantillas HTML.

## Ubicación de Plantillas

Las plantillas HTML están ubicadas en: `/src/main/resources/templates/mailTempo/`

## Tipos de Correos Implementados

### 1. Activación con Token (`activacion-con-token.html`)
- **Método:** `enviarCorreoActivacionConToken(Usuario usuario, String token)`
- **Descripción:** Se envía cuando un usuario PENDIENTE es activado por primera vez para que cree su contraseña.
- **Variables de plantilla:**
  - `nombreCompleto`: Nombre completo del usuario
  - `usuario`: Username del usuario
  - `resetPasswordUrl`: URL completa para restablecer contraseña (incluye token)
- **Validez del enlace:** 24 horas
- **Usado en:** `UsuarioActivationService`

### 2. Reactivación de Cuenta (`reactivacion.html`)
- **Método:** `enviarCorreoReactivacion(Usuario usuario)`
- **Descripción:** Se envía cuando un usuario INACTIVO es reactivado y puede acceder nuevamente al sistema.
- **Variables de plantilla:**
  - `nombreCompleto`: Nombre completo del usuario
  - `usuario`: Username del usuario
  - `frontendUrl`: URL del frontend del sistema
- **Usado en:** `UsuarioActivationService`

### 3. Restablecimiento de Contraseña (`restablecimiento-password.html`)
- **Método:** `enviarCorreoRestablecimiento(Usuario usuario, String token)`
- **Descripción:** Se envía cuando un usuario solicita restablecer su contraseña.
- **Variables de plantilla:**
  - `nombreCompleto`: Nombre completo del usuario
  - `resetPasswordUrl`: URL completa para restablecer contraseña (incluye token)
- **Validez del enlace:** 1 hora
- **Usado en:** `PasswordResetService`

### 4. Código de Verificación 2FA (`codigo-2fa.html`)
- **Método:** `sendTwoFactorCode(String email, String codigo, String nombreCompleto)`
- **Descripción:** Se envía cuando un usuario requiere autenticación de dos factores y se genera un código de verificación por email.
- **Variables de plantilla:**
  - `nombreCompleto`: Nombre completo del usuario
  - `codigo`: Código de verificación de 6 dígitos
- **Validez del código:** 10 minutos
- **Usado en:** `TwoFactorAuthService`

### 5. Recordatorio de Solicitud (`recordatorio-solicitud.html`)
- **Método:** `enviarRecordatorioSolicitud(String email, Integer numeroSolicitud)`
- **Descripción:** Se envía como recordatorio a los destinatarios de una solicitud pendiente.
- **Variables de plantilla:**
  - `numeroSolicitud`: ID/Número de la solicitud
  - `frontendUrl`: URL del frontend del sistema
- **Usado en:** `SolicitudRecordatorioService`

### 6. Notificación de Nueva Solicitud (`nueva-solicitud.html`)
- **Método:** `enviarNotificacionNuevaSolicitud(String email, Integer numeroSolicitud, String nombreSolicitud, String nombreSolicitante, boolean esOrdenSecuencial, boolean esSiguienteAprobador)`
- **Descripción:** Se envía a los aprobadores cuando se crea una nueva solicitud que requiere su revisión.
- **Variables de plantilla:**
  - `numeroSolicitud`: ID/Número de la solicitud
  - `nombreSolicitud`: Nombre de la solicitud
  - `nombreSolicitante`: Nombre completo del solicitante
  - `tipoAprobacion`: Tipo de aprobación (SECUENCIAL o SIMULTÁNEA)
  - `instrucciones`: Instrucciones específicas según el tipo de aprobación
  - `esOrdenSecuencial`: Boolean indicando si es orden secuencial
  - `esSiguienteAprobador`: Boolean indicando si es el siguiente aprobador
  - `frontendUrl`: URL del frontend del sistema
- **Usado en:** `SolicitudService`

### 7. Alerta de Login Sospechoso (`alerta-login.html`)
- **Método:** `sendLoginAttemptAlert(String email, String nombreCompleto, String ipAddress)`
- **Descripción:** Se envía cuando se detectan múltiples intentos de login fallidos en una cuenta.
- **Variables de plantilla:**
  - `nombreCompleto`: Nombre completo del usuario
  - `ipAddress`: Dirección IP desde donde se intentó el login
  - `fechaIntento`: Fecha y hora del intento (formato: yyyy-MM-dd HH:mm:ss)
  - `frontendUrl`: URL del frontend del sistema
- **Usado en:** `LoginAttemptService`

## Implementación Técnica

### Tecnologías Utilizadas
- **Thymeleaf:** Motor de plantillas para procesar las plantillas HTML
- **Spring Mail:** Para el envío de correos electrónicos
- **MimeMessageHelper:** Para crear mensajes HTML multipart

### Configuración
- Las plantillas se procesan mediante `TemplateEngine` de Thymeleaf
- El método auxiliar `enviarCorreoHtml()` centraliza el envío de correos HTML
- Todos los correos se envían en formato HTML con codificación UTF-8

### Estructura de Plantillas
Todas las plantillas siguen un diseño consistente:
- Header con gradiente de colores según el tipo de correo
- Contenido principal con la información específica
- Botones de acción cuando aplica
- Footer con información del sistema
- Diseño responsive y compatible con clientes de correo más comunes

## Notas de Desarrollo

- Las plantillas están diseñadas para ser compatibles con la mayoría de clientes de correo
- Se utilizan estilos inline para máxima compatibilidad
- Los colores y estilos varían según el tipo de correo para facilitar la identificación visual
- Todas las plantillas incluyen validación de variables null
