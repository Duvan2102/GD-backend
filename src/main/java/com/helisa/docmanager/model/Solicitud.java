package com.helisa.docmanager.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idSolicitud;

    @NotNull
    private String nombreSolicitud;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_tipologia")
    private Tipologia tipologia;

    private String detallesAdicionales;
    private String prioridad;
    private Boolean enviarRecordatorio;
    private String documentoPrincipal;
    private String documentosAdicionales;
    private String destinatarios;
    private String ordenFirma;

    private LocalDateTime fechaRegistro;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_estado")
    private Estado estado;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_solicitante")
    private Usuario solicitante;

    @JsonIgnore
    @OneToMany(mappedBy = "solicitud")
    private List<Comentario> comentarios;

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

    public Tipologia getTipologia() {
        return tipologia;
    }

    public void setTipologia(Tipologia tipologia) {
        this.tipologia = tipologia;
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

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Usuario solicitante) {
        this.solicitante = solicitante;
    }

    public List<Comentario> getComentarios() {
        return comentarios;
    }

    public void setComentarios(List<Comentario> comentarios) {
        this.comentarios = comentarios;
    }
}
