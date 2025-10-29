# Análisis de Correos Electrónicos - DocManager

## Resumen
Este documento lista todos los correos electrónicos que se envían actualmente en el sistema.

## Correos Identificados

### 1. Notificación de Nueva Solicitud
**Método**: `enviarNotificacionNuevaSolicitud()`  
**Ubicación**: `SolicitudService.java`  
**Llamado desde**:
- `enviarNotificacionesNuevaSolicitud()` - Cuando se crea una nueva solicitud
- `notificarSiguienteAprobador()` - Cuando un aprobador aprueba en orden secuencial y se debe notificar al siguiente

**Parámetros**:
- `destinatarioEmail` (String): Correo del destinatario
- `solicitudId` (Integer): ID de la solicitud
- `nombreSolicitud` (String): Nombre de la solicitud
- `nombreSolicitante` (String): Nombre completo del solicitante
- `esOrdenSecuencial` (Boolean): Indica si la aprobación es en orden secuencial
- `esSiguienteAprobador` (Boolean): Indica si es notificación al siguiente aprobador en orden secuencial

**Escenarios**:
1. **Aprobación Simultánea**: Se notifica a todos los destinatarios cuando se crea la solicitud
   - `esOrdenSecuencial = false`
   - `esSiguienteAprobador = false`

2. **Aprobación Secuencial - Primer Aprobador**: Se notifica solo al primer aprobador cuando se crea la solicitud
   - `esOrdenSecuencial = true`
   - `esSiguienteAprobador = true`

3. **Aprobación Secuencial - Siguiente Aprobador**: Se notifica al siguiente aprobador cuando el anterior aprueba
   - `esOrdenSecuencial = true`
   - `esSiguienteAprobador = true`

**Plantilla**: `nueva-solicitud.html`
