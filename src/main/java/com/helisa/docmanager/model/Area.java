package com.helisa.docmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "areas")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idArea;

    @NotNull
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "id_departamento")
    private Departamento departamento;

    @JsonIgnore
    @OneToMany(mappedBy = "area")
    private List<Cargo> cargos;
}
