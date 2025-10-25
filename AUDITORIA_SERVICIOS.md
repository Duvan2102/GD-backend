# Servicios de Auditoría - Document Manager

## Resumen

Se han implementado dos nuevos servicios de auditoría para el sistema de gestión documental:

1. **Servicio de Consulta de Auditoría**: Permite filtrar y consultar solicitudes con criterios específicos
2. **Servicio de Generación de Excel**: Genera reportes en formato Excel con historial completo de solicitudes

## Análisis del Flujo de Solicitudes

### Modelo de Datos
- **Solicitud**: Entidad principal con información básica, tipología, estado, solicitante y fecha de registro
- **Comentario**: Historial de cambios con usuario, descripción y fecha de registro
- **Usuario**: Información del solicitante con cargo y área
- **Cargo**: Relacionado con área
- **Area**: Relacionada con departamento
- **Departamento**: Nivel organizacional superior
- **Tipologia**: Tipo de solicitud relacionada con cargo
- **Estado**: Estado actual de la solicitud

### Flujo Identificado
1. **Crear**: Se crea una solicitud con estado inicial, tipología y usuario
2. **Consultar**: Se pueden consultar solicitudes con filtros
3. **Modificar**: Los cambios se registran como comentarios en el historial

## Servicios Implementados

### 1. Servicio de Consulta de Auditoría

**Endpoint**: `POST /api/audit/solicitudes`

**Request**:
```json
{
  "filters": {
    "fechas": {
      "fechaDesde": "2024-01-01",
      "fechaHasta": "2024-12-31"
    },
    "solicitante": "Juan Pérez",
    "estado": "APROBADO",
    "tipologia": "VACACIONES",
    "departamento": "ADMINISTRACIÓN"
  }
}
```

**Características**:
- Filtros opcionales (mínimo uno requerido)
- Búsqueda por rango de fechas
- Filtro por nombre del solicitante (búsqueda parcial)
- Filtro por estado exacto
- Filtro por tipología exacta
- Filtro por departamento exacto
- Respuesta incluye información completa de la solicitud

**Response**:
```json
[
  {
    "idSolicitud": 1,
    "nombreSolicitud": "Solicitud de Vacaciones",
    "tipologia": "VACACIONES",
    "estado": "APROBADO",
    "solicitante": "Juan Pérez",
    "departamento": "ADMINISTRACIÓN",
    "area": "RRHH",
    "cargo": "Analista",
    "fechaRegistro": "2024-01-15T10:30:00",
    "detallesAdicionales": "Vacaciones familiares",
    "prioridad": "MEDIA",
    "enviarRecordatorio": true,
    "documentoPrincipal": "solicitud_vacaciones.pdf",
    "documentosAdicionales": "certificado_medico.pdf",
    "destinatarios": "jefe@empresa.com",
    "ordenFirma": "1,2,3"
  }
]
```

### 2. Servicio de Generación de Excel

**Endpoint**: `POST /api/audit/excel`

**Request**:
```json
{
  "selectedIds": ["70", "81", "101"]
}
```

**Características**:
- Genera archivo Excel con información detallada
- Incluye historial completo de comentarios
- Formato profesional con estilos
- Columnas ajustables automáticamente
- Nombre de archivo con timestamp

**Contenido del Excel**:
- Información básica de la solicitud
- Datos del solicitante (nombre, departamento, área, cargo)
- Historial de comentarios con usuario y fecha
- Formato de fecha legible (dd/MM/yyyy HH:mm)

## Archivos Implementados

### DTOs
- `AuditFiltersRequest.java`: Request para filtros de auditoría
- `ExcelAuditRequest.java`: Request para generación de Excel
- `AuditSolicitudResponse.java`: Response con información completa de solicitud

### Servicios
- `AuditService.java`: Lógica de negocio para consultas de auditoría
- `ExcelAuditService.java`: Generación de archivos Excel
- `SolicitudService.java`: Servicio básico para CRUD de solicitudes

### Controladores
- `AuditController.java`: Endpoints de auditoría
- `SolicitudController.java`: Endpoints básicos de solicitudes

### Repositorios
- `SolicitudAuditRepository.java`: Consultas personalizadas para auditoría

### Configuración
- `SecurityConfig.java`: Configuración de seguridad con endpoints de auditoría

## Dependencias Agregadas

```xml
<!-- Apache POI para generar archivos Excel -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.4</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.4</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-scratchpad</artifactId>
    <version>5.2.4</version>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

## Seguridad

- Todos los endpoints de auditoría requieren autenticación
- Configuración CORS habilitada
- Validación de entrada con anotaciones de Bean Validation

## Consideraciones de Rendimiento

- Consultas optimizadas con JOIN FETCH para evitar N+1 queries
- Filtros indexados por fecha de registro
- Paginación recomendada para grandes volúmenes de datos
- Generación de Excel en memoria con liberación automática de recursos

## Uso Recomendado

1. **Para consultas frecuentes**: Usar el endpoint de consulta con filtros específicos
2. **Para reportes**: Usar el endpoint de Excel con IDs seleccionados
3. **Para auditoría completa**: Combinar ambos servicios según necesidades

## Próximos Pasos Sugeridos

1. Implementar paginación en el servicio de consulta
2. Agregar caché para consultas frecuentes
3. Implementar logs de auditoría para el uso de los servicios
4. Agregar métricas de rendimiento
5. Implementar validaciones adicionales de seguridad