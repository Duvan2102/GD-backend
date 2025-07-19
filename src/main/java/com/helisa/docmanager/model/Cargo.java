package com.helisa.docmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
@Entity
@Table(name = "cargos")
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCargo;

    @NotNull
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "id_area")
    private Area area;

    @JsonIgnore
    @OneToMany(mappedBy = "cargo")
    private List<Usuario> usuarios;

    @JsonIgnore
    @OneToMany(mappedBy = "cargo")
    private List<Tipologia> tipologias;
}
