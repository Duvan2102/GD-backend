# Servicios de Auditoría de Solicitudes

## Descripción

Se han implementado dos nuevos servicios de auditoría para el sistema de gestión de solicitudes:

1. **Servicio de Búsqueda de Auditoría**: Permite buscar solicitudes con filtros avanzados
2. **Servicio de Generación de Excel**: Genera reportes en Excel con historial completo de solicitudes

## Endpoints Disponibles

### 1. Búsqueda de Solicitudes de Auditoría

**POST** `/api/auditoria/buscar`

Permite buscar solicitudes aplicando filtros específicos. Al menos uno de los filtros debe estar presente.

#### Request Body:
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

#### Parámetros de Query:
- `page`: Número de página (default: 0)
- `size`: Tamaño de página (default: 20)
- `sort`: Campo de ordenamiento (default: createdAt)
- `direction`: Dirección de ordenamiento (default: DESC)

#### Response:
```json
{
  "content": [
    {
      "id": 70,
      "nombreSolicitud": "Solicitud de Vacaciones",
      "estado": "APROBADO",
      "tipologia": "VACACIONES",
      "departamento": "ADMINISTRACIÓN",
      "solicitanteNombre": "Juan Pérez",
      "solicitanteCargo": "Analista",
      "fechaRegistro": "2024-01-15T10:30:00",
      "createdAt": "2024-01-15T10:30:00",
      "createdBy": 123,
      "ordenFirma": false,
      "prioridad": false,
      "enviarRecordatorio": 0,
      "destinatariosTotal": 3,
      "destinatariosAprobados": 3,
      "pdfOriginalName": "solicitud_vacaciones.pdf",
      "pdfSizeBytes": 1024000,
      "destinatarios": [...],
      "historial": [...]
    }
  ],
  "pageable": {...},
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "size": 20,
  "number": 0
}
```

### 2. Generación de Excel de Auditoría

**POST** `/api/auditoria/excel`

Genera un archivo Excel con la información completa de las solicitudes seleccionadas, incluyendo el historial de cada una.

#### Request Body:
```json
{
  "selectedIds": ["70", "81", "101"]
}
```

#### Response:
- **Content-Type**: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Content-Disposition**: `attachment; filename="auditoria_solicitudes_YYYYMMDD_HHMMSS.xlsx"`
- **Body**: Archivo Excel binario

## Estructura del Excel Generado

El archivo Excel contiene las siguientes columnas:

### Información de la Solicitud:
- ID Solicitud
- Nombre Solicitud
- Estado
- Tipología
- Departamento
- Solicitante
- Cargo Solicitante
- Fecha Registro
- Orden Firma
- Prioridad
- Total Destinatarios
- Aprobados
- PDF Original
- Tamaño PDF (bytes)

### Información del Historial:
- Tipo Registro (SOLICITUD/HISTORIAL)
- Usuario
- Acción
- Comentario
- Fecha

## Filtros Disponibles

### Filtro de Fechas
- `fechaDesde`: Fecha de inicio (formato: YYYY-MM-DD)
- `fechaHasta`: Fecha de fin (formato: YYYY-MM-DD)
- Se puede usar solo una de las dos fechas

### Filtro de Solicitante
- `solicitante`: Nombre completo del solicitante (búsqueda parcial)

### Filtro de Estado
- `estado`: Estado de la solicitud (PENDIENTE, APROBADO, RECHAZADO, CANCELADA)

### Filtro de Tipología
- `tipologia`: Descripción de la tipología

### Filtro de Departamento
- `departamento`: Descripción del departamento

## Validaciones

1. **Búsqueda de Auditoría**: Debe proporcionar al menos un filtro
2. **Generación de Excel**: Debe proporcionar al menos un ID de solicitud válido
3. **Fechas**: Si se proporcionan ambas fechas, `fechaDesde` debe ser anterior a `fechaHasta`

## Consideraciones de Rendimiento

- La búsqueda está paginada para manejar grandes volúmenes de datos
- El Excel se genera en memoria para mejor rendimiento
- Se recomienda limitar el número de solicitudes en el Excel a un máximo de 1000

## Ejemplos de Uso

### Ejemplo 1: Buscar solicitudes del último mes
```bash
curl -X POST "http://localhost:8080/api/auditoria/buscar" \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "fechas": {
        "fechaDesde": "2024-01-01",
        "fechaHasta": "2024-01-31"
      }
    }
  }'
```

### Ejemplo 2: Buscar solicitudes aprobadas de un departamento específico
```bash
curl -X POST "http://localhost:8080/api/auditoria/buscar" \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "estado": "APROBADO",
      "departamento": "ADMINISTRACIÓN"
    }
  }'
```

### Ejemplo 3: Generar Excel para solicitudes específicas
```bash
curl -X POST "http://localhost:8080/api/auditoria/excel" \
  -H "Content-Type: application/json" \
  -d '{
    "selectedIds": ["70", "81", "101"]
  }' \
  --output auditoria.xlsx
```

## Notas Técnicas

- Los servicios están optimizados para consultas de solo lectura
- Se utiliza paginación para evitar problemas de memoria
- El Excel incluye formato y estilos para mejor legibilidad
- Se registran logs detallados para auditoría y debugging