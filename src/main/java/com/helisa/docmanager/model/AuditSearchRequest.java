package com.helisa.docmanager.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AuditSearchRequest {
    
    @NotNull
    private Filters filters;
    
    @Data
    public static class Filters {
        private Fechas fechas;
        private String solicitante;
        private String estado;
        private String tipologia;
        private String departamento;
        
        @Data
        public static class Fechas {
            private LocalDate fechaDesde;
            private LocalDate fechaHasta;
        }
    }
}
