package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Documento del Expediente de Matrícula (DNI escaneado, Partida de Nacimiento,
 * certificado de estudios, etc.) subido durante el proceso de Matrícula Digital.
 */
@Entity
@Table(name = "expediente_documentos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ExpedienteDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    @NotNull
    private Alumno alumno;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 40)
    @NotNull
    private TipoDocumento tipoDocumento;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    @NotBlank
    private String nombreArchivo;

    @Column(name = "ruta_storage", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String rutaStorage;

    @Column(name = "url_publica", columnDefinition = "TEXT")
    private String urlPublica;

    @Column(name = "tipo_contenido", length = 100)
    private String tipoContenido;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_verificacion", length = 20)
    @Builder.Default
    private EstadoVerificacion estadoVerificacion = EstadoVerificacion.PENDIENTE;

    @Column(name = "observaciones_verificacion", length = 300)
    private String observacionesVerificacion;

    @Column(name = "anio_matricula")
    private Integer anioMatricula;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Enums ─────────────────────────────────────────────────────────────
    public enum TipoDocumento {
        DNI,
        PARTIDA_NACIMIENTO,
        CERTIFICADO_ESTUDIOS,
        LIBRETA_NOTAS_ANTERIOR,
        FOTO_CARNET,
        CONSTANCIA_SALUD,
        FICHA_MATRICULA,
        OTRO
    }

    public enum EstadoVerificacion {
        PENDIENTE,
        VERIFICADO,
        RECHAZADO,
        REQUIERE_RESUBIDA
    }
}
