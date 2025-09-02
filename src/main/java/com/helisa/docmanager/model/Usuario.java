package com.helisa.docmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Data
@Entity
@Table(name = "usuarios")
@ToString(exclude = {"password"})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idUsuario;

    @NotNull
    private String identificacion;

    @NotNull
    private String nombres;

    @NotNull
    private String apellidos;

    @NotNull
    @Column(unique = true)
    private String usuario;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "id_cargo")
    private Cargo cargo;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    private String correoEmpresarial;
    private String correoPersonal;
    private String telefono1;
    private String telefono2;
    private String direccion;

    private Boolean dobleAutenticacion;
    @JsonIgnore
    @OneToMany(mappedBy = "usuario")
    private List<Token> tokens;
    @JsonIgnore
    @OneToMany(mappedBy = "usuario")
    private List<Comentario> comentarios;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;

}
