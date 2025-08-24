package com.helisa.docmanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "solicitud_historial", indexes = {
        @Index(name = "idx_hist_solicitud", columnList = "solicitud_id")
})
public class SolicitudHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @Column(name = "actor_usuario_id")
    private Long actorUsuarioId;

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
        CREAR, APROBAR, RECHAZAR, CANCELAR, ADJUNTAR
    }
}
