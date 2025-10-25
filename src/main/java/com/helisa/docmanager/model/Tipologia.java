package com.helisa.docmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Entity
@Table(name = "tipologia")
public class Tipologia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTipologia;

    @NotNull
    private String descripcion;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_cargo")
    private Cargo cargo;

    @JsonIgnore
    @OneToMany(mappedBy = "tipologia")
    private List<Solicitud> solicitudes;

    // Getters y Setters
    public Integer getIdTipologia() {
        return idTipologia;
    }

    public void setIdTipologia(Integer idTipologia) {
        this.idTipologia = idTipologia;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    public List<Solicitud> getSolicitudes() {
        return solicitudes;
    }

    public void setSolicitudes(List<Solicitud> solicitudes) {
        this.solicitudes = solicitudes;
    }
}
