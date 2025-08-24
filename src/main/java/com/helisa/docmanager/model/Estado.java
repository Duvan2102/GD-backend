package com.helisa.docmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "estado")
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEstado;

    @NotNull
    private String descripcion;

    @JsonIgnore
    @OneToMany(mappedBy = "estado")
    private List<Solicitud> solicitudes;
}
