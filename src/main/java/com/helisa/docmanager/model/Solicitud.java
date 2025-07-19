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

    @JsonIgnore
    @OneToMany(mappedBy = "solicitud")
    private List<Comentario> comentarios;
}
