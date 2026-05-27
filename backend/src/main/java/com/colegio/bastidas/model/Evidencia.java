package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Evidencia digital asociada a una nota o evaluación.
 * Soporta fotos de exámenes y trabajos subidos desde dispositivos móviles.
 * Los archivos se almacenan en Supabase Storage.
 */
@Entity
@Table(name = "evidencias", indexes = {
    @Index(name = "idx_evidencia_nota", columnList = "nota_id"),
    @Index(name = "idx_evidencia_alumno", columnList = "alumno_id"),
    @Index(name = "idx_evidencia_docente", columnList = "docente_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Evidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relaciones ────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_id")
    private Nota nota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    @NotNull
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    @NotNull
    private Docente docente;

    // ── Datos del Archivo ─────────────────────────────────────────────────
    @Column(name = "nombre_archivo", nullable = false, length = 255)
    @NotBlank
    private String nombreArchivo;

    /** Nombre original del archivo tal como llegó desde el cliente. */
    @Column(name = "nombre_original", length = 255)
    private String nombreOriginal;

    /** Ruta relativa dentro del bucket de Supabase Storage. */
    @Column(name = "ruta_storage", nullable = false)
    @NotBlank
    private String rutaStorage;

    /** URL pública o firmada para acceder al archivo en Supabase Storage. */
    @Column(name = "url_publica", columnDefinition = "TEXT")
    private String urlPublica;

    @Column(name = "tipo_contenido", length = 100)
    private String tipoContenido; // MIME type (image/jpeg, application/pdf…)

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    // ── Metadatos Educativos ──────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evidencia", length = 30)
    @Builder.Default
    private TipoEvidencia tipoEvidencia = TipoEvidencia.EXAMEN;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "fecha_evaluacion")
    private LocalDate fechaEvaluacion;

    @Column(name = "periodo_academico", length = 10)
    private String periodoAcademico; // "B1", "B2", "B3", "B4"

    // ── Auditoría ─────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Enum ──────────────────────────────────────────────────────────────
    public enum TipoEvidencia {
        EXAMEN,
        PRACTICA,
        TRABAJO_GRUPAL,
        EXPOSICION,
        PROYECTO,
        TAREA,
        OTRO
    }
}
