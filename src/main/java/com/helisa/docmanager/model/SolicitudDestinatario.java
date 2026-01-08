package com.helisa.docmanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "solicitud_destinatario", indexes = {
        @Index(name = "idx_dest_solicitud", columnList = "solicitud_id"),
        @Index(name = "idx_dest_usuario", columnList = "usuario_id"),
        @Index(name = "idx_dest_decision", columnList = "decision"),
        @Index(name = "idx_dest_orden", columnList = "solicitud_id, orden_index"),
        @Index(name = "idx_dest_es_procesador", columnList = "es_procesador")
})
public class SolicitudDestinatario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @NotNull
    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @NotNull
    @Column(name = "orden_index", nullable = false)
    private Integer ordenIndex;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private DecisionEnum decision = DecisionEnum.PENDIENTE;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;

    @NotNull
    @Column(name = "es_procesador", nullable = false)
    private Boolean esProcesador = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DecisionEnum {
        PENDIENTE, APROBADO, RECHAZADO, CANCELADO
    }

    public boolean estaPendiente() {
        return DecisionEnum.PENDIENTE.equals(this.decision);
    }

    public boolean haDecidido() {
        return !DecisionEnum.PENDIENTE.equals(this.decision);
    }

    public void aprobar(String comentario) {
        this.decision = DecisionEnum.APROBADO;
        this.comentario = comentario;
        this.fechaDecision = LocalDateTime.now();
    }

    public void rechazar(String comentario) {
        this.decision = DecisionEnum.RECHAZADO;
        this.comentario = comentario;
        this.fechaDecision = LocalDateTime.now();
    }
}
