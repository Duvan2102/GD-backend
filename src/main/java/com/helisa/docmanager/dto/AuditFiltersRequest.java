package com.helisa.docmanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AuditFiltersRequest {
    
    @JsonProperty("filters")
    @NotNull
    private Filters filters;
    
    public static class Filters {
        private Fechas fechas;
        private String solicitante;
        private String estado;
        private String tipologia;
        private String departamento;
        
        public static class Fechas {
            @JsonProperty("fechaDesde")
            private LocalDate fechaDesde;
            
            @JsonProperty("fechaHasta")
            private LocalDate fechaHasta;
            
            // Getters y Setters
            public LocalDate getFechaDesde() {
                return fechaDesde;
            }
            
            public void setFechaDesde(LocalDate fechaDesde) {
                this.fechaDesde = fechaDesde;
            }
            
            public LocalDate getFechaHasta() {
                return fechaHasta;
            }
            
            public void setFechaHasta(LocalDate fechaHasta) {
                this.fechaHasta = fechaHasta;
            }
        }
        
        // Getters y Setters
        public Fechas getFechas() {
            return fechas;
        }
        
        public void setFechas(Fechas fechas) {
            this.fechas = fechas;
        }
        
        public String getSolicitante() {
            return solicitante;
        }
        
        public void setSolicitante(String solicitante) {
            this.solicitante = solicitante;
        }
        
        public String getEstado() {
            return estado;
        }
        
        public void setEstado(String estado) {
            this.estado = estado;
        }
        
        public String getTipologia() {
            return tipologia;
        }
        
        public void setTipologia(String tipologia) {
            this.tipologia = tipologia;
        }
        
        public String getDepartamento() {
            return departamento;
        }
        
        public void setDepartamento(String departamento) {
            this.departamento = departamento;
        }
    }
    
    // Getters y Setters
    public Filters getFilters() {
        return filters;
    }
    
    public void setFilters(Filters filters) {
        this.filters = filters;
    }
}