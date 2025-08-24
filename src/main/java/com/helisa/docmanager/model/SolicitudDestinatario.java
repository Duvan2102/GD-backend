package com.helisa.docmanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "solicitud_destinatario", indexes = {
        @Index(name = "idx_dest_solicitud", columnList = "solicitud_id"),
        @Index(name = "idx_dest_usuario", columnList = "usuario_id")
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
    private Integer usuarioId; // ✅ CAMBIAR A Integer PARA CONSISTENCIA

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

    public enum DecisionEnum {
        PENDIENTE, APROBADO, RECHAZADO, CANCELADO
    }
}
