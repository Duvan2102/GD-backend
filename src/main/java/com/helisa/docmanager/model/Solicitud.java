package com.helisa.docmanager.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "solicitud", indexes = {
        @Index(name = "idx_solicitud_estado", columnList = "estado"),
        @Index(name = "idx_solicitud_solicitante", columnList = "id_solicitante"),
        @Index(name = "idx_solicitud_tipologia", columnList = "tipologia_id")
})
@EqualsAndHashCode(exclude = {"destinatarios", "adjuntos", "historial"})
@ToString(exclude = {"destinatarios", "adjuntos", "historial"})
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "id_solicitante", nullable = false)
    private Long idSolicitante;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @NotNull
    @Column(name = "tipologia_id", nullable = false)
    private Long tipologiaId;

    // ✅ AGREGAR ESTA RELACIÓN PARA COMPATIBILIDAD CON TIPOLOGIA EXISTENTE
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipologia_id", insertable = false, updatable = false)
    private Tipologia tipologia;

    @NotNull
    @Column(name = "orden_firma", nullable = false)
    private Boolean ordenFirma;

    @NotNull
    @Column(name = "pdf_path", nullable = false)
    private String pdfPath;

    @NotNull
    @Column(name = "pdf_original_name", nullable = false)
    private String pdfOriginalName;

    @NotNull
    @Column(name = "pdf_size_bytes", nullable = false)
    private Long pdfSizeBytes = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Integer version;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SolicitudDestinatario> destinatarios;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SolicitudAdjunto> adjuntos;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SolicitudHistorial> historial;

    public enum EstadoSolicitud {
        PENDIENTE, APROBADO, RECHAZADO, CANCELADA
    }
}