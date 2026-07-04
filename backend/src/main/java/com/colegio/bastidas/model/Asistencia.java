package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidad de Asistencia diaria del alumno.
 * Soporta la lógica de entrada diferida para alumnos con Permiso de Academia
 * (entrada permitida a las 13:30 o 14:30 hs.).
 */
@Entity
@Table(name = "asistencias", indexes = {
    @Index(name = "idx_asistencia_alumno_fecha", columnList = "alumno_id, fecha"),
    @Index(name = "idx_asistencia_fecha", columnList = "fecha"),
    @Index(name = "idx_asistencia_docente", columnList = "docente_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relaciones ────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    @NotNull
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aula_id")
    private Aula aula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docente_id")
    private Docente docente; // Docente que registró la asistencia

    // ── Datos de la Asistencia ────────────────────────────────────────────
    @Column(nullable = false)
    @NotNull
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull
    @Builder.Default
    private EstadoAsistencia estado = EstadoAsistencia.ASISTIO;

    /** Hora real de llegada registrada por el docente vía app móvil. */
    @Column(name = "hora_llegada")
    private LocalTime horaLlegada;

    /** Hora oficial de entrada del turno (normalmente 07:30). */
    @Column(name = "hora_entrada_turno")
    private LocalTime horaEntradaTurno;

    /** Si fue tarde, indica los minutos de tardanza calculados. */
    @Column(name = "minutos_tardanza")
    private Integer minutosTardanza;

    // ── Lógica de Permiso de Academia ─────────────────────────────────────
    /**
     * TRUE cuando el registro se realizó bajo la dispensa de academia.
     * El alumno puede ingresar a las 13:30 o 14:30 sin marcarse TARDANZA.
     */
    @Column(name = "aplicado_permiso_academia")
    @Builder.Default
    private Boolean aplicadoPermisoAcademia = false;

    @Column(name = "hora_permiso_academia", length = 10)
    private String horaPermisoAcademia; // "13:30" o "14:30"

    // ── Justificación ─────────────────────────────────────────────────────
    @Column(length = 500)
    private String justificacion;

    @Column(name = "tiene_justificacion")
    @Builder.Default
    private Boolean tieneJustificacion = false;

    /** URL de la evidencia (archivo adjunto de justificación). */
    @Column(name = "url_evidencia_justificacion")
    private String urlEvidenciaJustificacion;

    // ── Sincronización Offline ────────────────────────────────────────────
    /**
     * Indica que el registro fue creado offline en el dispositivo móvil
     * y sincronizado posteriormente con el servidor.
     */
    @Column(name = "sincronizado_offline")
    @Builder.Default
    private Boolean sincronizadoOffline = false;

    @Column(name = "timestamp_registro_local")
    private LocalDateTime timestampRegistroLocal;

    // ── Auditoría ─────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Enum ──────────────────────────────────────────────────────────────
    public enum EstadoAsistencia {
        ASISTIO,
        FALTA,
        TARDANZA,
        TARDANZA_JUSTIFICADA,
        LICENCIA,
        PERMISO_ACADEMIA,
        JUSTIFICADO
    }
}
