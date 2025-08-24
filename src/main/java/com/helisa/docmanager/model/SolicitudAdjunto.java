package com.helisa.docmanager.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Data
@Entity
@Table(name = "solicitud_adjunto", indexes = {
        @Index(name = "idx_adj_solicitud", columnList = "solicitud_id"),
        @Index(name = "idx_adj_created", columnList = "created_at")
})
public class SolicitudAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @NotNull
    @Column(name = "path", nullable = false)
    private String path;

    @NotNull
    @Column(name = "original_name", nullable = false)
    private String originalName;

    @NotNull
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes = 0L;

    @Column(name = "mime", length = 100)
    private String mime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Métodos de utilidad
    public String getSizeFormatted() {
        if (sizeBytes < 1024) return sizeBytes + " B";
        if (sizeBytes < 1024 * 1024) return (sizeBytes / 1024) + " KB";
        return (sizeBytes / 1024 / 1024) + " MB";
    }

    public boolean isPdf() {
        return "application/pdf".equals(this.mime);
    }

    public boolean isImage() {
        return this.mime != null && this.mime.startsWith("image/");
    }
}
