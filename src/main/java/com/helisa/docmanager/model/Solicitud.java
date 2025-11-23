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
@Table(name = "solicitudes", indexes = {
        @Index(name = "idx_solicitud_estado", columnList = "id_estado"),
        @Index(name = "idx_solicitud_solicitante", columnList = "id_solicitante"),
        @Index(name = "idx_solicitud_tipologia", columnList = "id_tipologia")
})
@EqualsAndHashCode(exclude = {"estado", "destinatariosDetalle", "adjuntos", "historial", "tipologia"})
@ToString(exclude = {"estado", "destinatariosDetalle", "adjuntos", "historial", "tipologia"})
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_solicitud")
    private String nombreSolicitud;

    @NotNull
    @Column(name = "id_solicitante", nullable = false)
    private Integer idSolicitante;

    @NotNull
    @Column(name = "id_tipologia", nullable = false)
    private Integer idTipologia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipologia", insertable = false, updatable = false)
    private Tipologia tipologia;

    @Column(name = "prioridad")
    private Boolean prioridad;

    @Column(name = "enviar_recordatorio")
    private Integer enviarRecordatorio;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    // ===== RELACIÓN CORRECTA CON ESTADO =====
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado", nullable = false)
    private Estado estado;

    @NotNull
    @Column(name = "orden_firma_boolean", nullable = false)
    private Boolean ordenFirmaBoolean = false;

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
    private List<SolicitudDestinatario> destinatariosDetalle;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SolicitudAdjunto> adjuntos;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SolicitudHistorial> historial;

    // ===== CONSTANTES DE ESTADO =====
    public static final Integer ESTADO_PENDIENTE_ID = 1;
    public static final Integer ESTADO_APROBADO_ID = 2;
    public static final Integer ESTADO_RECHAZADO_ID = 3;
    public static final Integer ESTADO_CANCELADO_ID = 4;
    public static final Integer ESTADO_APROBADO_PROCESO_ID = 9;

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_APROBADO = "APROBADO";
    public static final String ESTADO_RECHAZADO = "RECHAZADO";
    public static final String ESTADO_CANCELADO = "CANCELADA";
    public static final String ESTADO_APROBADO_PROCESO = "APROBADO PROCESO";

    // ===== MÉTODOS DE UTILIDAD =====
    @PrePersist
    private void prePersist() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
        if (this.nombreSolicitud == null || this.nombreSolicitud.trim().isEmpty()) {
            this.nombreSolicitud = "Solicitud " + System.currentTimeMillis();
        }
    }

    public boolean esOrdenSecuencial() {
        return Boolean.TRUE.equals(this.ordenFirmaBoolean);
    }

    public boolean estaPendiente() {
        return this.estado != null && this.estado.getIdEstado().equals(ESTADO_PENDIENTE_ID);
    }

    public boolean estaAprobado() {
        return this.estado != null && this.estado.getIdEstado().equals(ESTADO_APROBADO_ID);
    }

    public boolean estaRechazado() {
        return this.estado != null && this.estado.getIdEstado().equals(ESTADO_RECHAZADO_ID);
    }

    public boolean estaCancelado() {
        return this.estado != null && this.estado.getIdEstado().equals(ESTADO_CANCELADO_ID);
    }

    public boolean estaFinalizado() {
        return !estaPendiente();
    }

    public String getEstadoDescripcion() {
        return this.estado != null ? this.estado.getDescripcion() : null;
    }

    public Integer getIdEstado() {
        return this.estado != null ? this.estado.getIdEstado() : null;
    }
}