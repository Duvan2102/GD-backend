package com.helisa.docmanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public class AuditSolicitudResponse {
    
    @JsonProperty("idSolicitud")
    private Integer idSolicitud;
    
    @JsonProperty("nombreSolicitud")
    private String nombreSolicitud;
    
    @JsonProperty("tipologia")
    private String tipologia;
    
    @JsonProperty("estado")
    private String estado;
    
    @JsonProperty("solicitante")
    private String solicitante;
    
    @JsonProperty("departamento")
    private String departamento;
    
    @JsonProperty("area")
    private String area;
    
    @JsonProperty("cargo")
    private String cargo;
    
    @JsonProperty("fechaRegistro")
    private LocalDateTime fechaRegistro;
    
    @JsonProperty("detallesAdicionales")
    private String detallesAdicionales;
    
    @JsonProperty("prioridad")
    private String prioridad;
    
    @JsonProperty("enviarRecordatorio")
    private Boolean enviarRecordatorio;
    
    @JsonProperty("documentoPrincipal")
    private String documentoPrincipal;
    
    @JsonProperty("documentosAdicionales")
    private String documentosAdicionales;
    
    @JsonProperty("destinatarios")
    private String destinatarios;
    
    @JsonProperty("ordenFirma")
    private String ordenFirma;
    
    @JsonProperty("historial")
    private List<HistorialComentario> historial;
    
    public static class HistorialComentario {
        @JsonProperty("idComentario")
        private Integer idComentario;
        
        @JsonProperty("descripcion")
        private String descripcion;
        
        @JsonProperty("usuario")
        private String usuario;
        
        @JsonProperty("fechaRegistro")
        private LocalDateTime fechaRegistro;
        
        // Getters y Setters
        public Integer getIdComentario() {
            return idComentario;
        }
        
        public void setIdComentario(Integer idComentario) {
            this.idComentario = idComentario;
        }
        
        public String getDescripcion() {
            return descripcion;
        }
        
        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }
        
        public String getUsuario() {
            return usuario;
        }
        
        public void setUsuario(String usuario) {
            this.usuario = usuario;
        }
        
        public LocalDateTime getFechaRegistro() {
            return fechaRegistro;
        }
        
        public void setFechaRegistro(LocalDateTime fechaRegistro) {
            this.fechaRegistro = fechaRegistro;
        }
    }
    
    // Getters y Setters
    public Integer getIdSolicitud() {
        return idSolicitud;
    }
    
    public void setIdSolicitud(Integer idSolicitud) {
        this.idSolicitud = idSolicitud;
    }
    
    public String getNombreSolicitud() {
        return nombreSolicitud;
    }
    
    public void setNombreSolicitud(String nombreSolicitud) {
        this.nombreSolicitud = nombreSolicitud;
    }
    
    public String getTipologia() {
        return tipologia;
    }
    
    public void setTipologia(String tipologia) {
        this.tipologia = tipologia;
    }
    
    public String getEstado() {
        return estado;
    }
    
    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    public String getSolicitante() {
        return solicitante;
    }
    
    public void setSolicitante(String solicitante) {
        this.solicitante = solicitante;
    }
    
    public String getDepartamento() {
        return departamento;
    }
    
    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }
    
    public String getArea() {
        return area;
    }
    
    public void setArea(String area) {
        this.area = area;
    }
    
    public String getCargo() {
        return cargo;
    }
    
    public void setCargo(String cargo) {
        this.cargo = cargo;
    }
    
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
    
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
    
    public String getDetallesAdicionales() {
        return detallesAdicionales;
    }
    
    public void setDetallesAdicionales(String detallesAdicionales) {
        this.detallesAdicionales = detallesAdicionales;
    }
    
    public String getPrioridad() {
        return prioridad;
    }
    
    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }
    
    public Boolean getEnviarRecordatorio() {
        return enviarRecordatorio;
    }
    
    public void setEnviarRecordatorio(Boolean enviarRecordatorio) {
        this.enviarRecordatorio = enviarRecordatorio;
    }
    
    public String getDocumentoPrincipal() {
        return documentoPrincipal;
    }
    
    public void setDocumentoPrincipal(String documentoPrincipal) {
        this.documentoPrincipal = documentoPrincipal;
    }
    
    public String getDocumentosAdicionales() {
        return documentosAdicionales;
    }
    
    public void setDocumentosAdicionales(String documentosAdicionales) {
        this.documentosAdicionales = documentosAdicionales;
    }
    
    public String getDestinatarios() {
        return destinatarios;
    }
    
    public void setDestinatarios(String destinatarios) {
        this.destinatarios = destinatarios;
    }
    
    public String getOrdenFirma() {
        return ordenFirma;
    }
    
    public void setOrdenFirma(String ordenFirma) {
        this.ordenFirma = ordenFirma;
    }
    
    public List<HistorialComentario> getHistorial() {
        return historial;
    }
    
    public void setHistorial(List<HistorialComentario> historial) {
        this.historial = historial;
    }
}