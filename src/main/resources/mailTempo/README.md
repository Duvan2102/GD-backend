# Plantillas de Correo Electrónico - DocManager

Este directorio contiene las plantillas HTML para los correos electrónicos enviados por el sistema.

## Estructura

Las plantillas utilizan **Thymeleaf** como motor de plantillas y se encuentran en formato HTML.

## Plantillas Disponibles

### 1. `nueva-solicitud.html`
Plantilla para notificar a los aprobadores sobre una nueva solicitud.

**Variables disponibles:**
- `destinatarioEmail` (String): Correo del destinatario
- `solicitudId` (Integer): ID de la solicitud
- `nombreSolicitud` (String): Nombre de la solicitud
- `nombreSolicitante` (String): Nombre completo del solicitante
- `esOrdenSecuencial` (Boolean): Indica si la aprobación es en orden secuencial
- `esSiguienteAprobador` (Boolean): Indica si es notificación al siguiente aprobador
- `baseUrl` (String): URL base de la aplicación

**Uso:**
```java
emailService.enviarNotificacionNuevaSolicitud(
    destinatarioEmail,
    solicitudId,
    nombreSolicitud,
    nombreSolicitante,
    esOrdenSecuencial,
    esSiguienteAprobador
);
```

## Cómo Agregar una Nueva Plantilla

1. Cree un archivo HTML en este directorio con el nombre de la plantilla (ej: `mi-nueva-plantilla.html`)
2. Use la sintaxis de Thymeleaf para las variables: `${variableName}`
3. Agregue el método correspondiente en `EmailService.java`
4. Documente la plantilla en este README

## Ejemplo de Plantilla

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Título del Correo</title>
</head>
<body>
    <h1 th:text="${titulo}">Título por defecto</h1>
    <p th:text="${mensaje}">Mensaje por defecto</p>
</body>
</html>
```

## Notas

- Las plantillas deben tener la extensión `.html`
- Thymeleaf procesa las plantillas en tiempo de ejecución
- El cache está desactivado en desarrollo, activo en producción
- Las plantillas deben ser responsive para una buena visualización en dispositivos móviles
