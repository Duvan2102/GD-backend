package com.helisa.docmanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "solicitud_historial", indexes = {
        @Index(name = "idx_hist_solicitud", columnList = "solicitud_id"),
        @Index(name = "idx_hist_fecha", columnList = "fecha"),
        @Index(name = "idx_hist_actor", columnList = "actor_usuario_id")
})
public class SolicitudHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @Column(name = "actor_usuario_id")
    private Integer actorUsuarioId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 20)
    private AccionEnum accion;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @NotNull
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public enum AccionEnum {
        CREAR, APROBAR, RECHAZAR, CANCELAR, ADJUNTAR, DESCARGAR
    }

    // Factory methods
    public static SolicitudHistorial crear(Solicitud solicitud, Integer usuarioId, AccionEnum accion, String comentario) {
        SolicitudHistorial historial = new SolicitudHistorial();
        historial.setSolicitud(solicitud);
        historial.setActorUsuarioId(usuarioId);
        historial.setAccion(accion);
        historial.setComentario(comentario);
        historial.setFecha(LocalDateTime.now());
        return historial;
    }

    public static SolicitudHistorial crearSolicitud(Solicitud solicitud, Integer creadorId, String comentario) {
        return crear(solicitud, creadorId, AccionEnum.CREAR, comentario);
    }

    public static SolicitudHistorial aprobar(Solicitud solicitud, Integer usuarioId, String comentario) {
        return crear(solicitud, usuarioId, AccionEnum.APROBAR, comentario);
    }

    public static SolicitudHistorial rechazar(Solicitud solicitud, Integer usuarioId, String comentario) {
        return crear(solicitud, usuarioId, AccionEnum.RECHAZAR, comentario);
    }
}
