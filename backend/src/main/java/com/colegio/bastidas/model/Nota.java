package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad de Nota por competencias del alumno.
 * Compatible con el sistema de evaluación por competencias del CNEB (Currículo
 * Nacional de Educación Básica) y exportable a formato SIAGIE.
 */
@Entity
@Table(name = "notas", indexes = {
    @Index(name = "idx_nota_alumno", columnList = "alumno_id"),
    @Index(name = "idx_nota_periodo", columnList = "alumno_id, periodo_academico, anio_academico")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relaciones ────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    @NotNull
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    @NotNull
    private Docente docente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aula_id")
    private Aula aula;

    // ── Datos Académicos ──────────────────────────────────────────────────
    @Column(name = "area_curricular", nullable = false, length = 100)
    @NotBlank
    private String areaCurricular; // "Matemática", "Comunicación", "CTA", etc.

    @Column(name = "competencia", length = 200)
    private String competencia; // Descripción de la competencia evaluada

    @Column(name = "periodo_academico", nullable = false, length = 10)
    @NotBlank
    private String periodoAcademico; // "B1", "B2", "B3", "B4" (bimestres)

    @Column(name = "anio_academico", nullable = false)
    @Min(2020) @Max(2100)
    @NotNull
    private Integer anioAcademico;

    // ── Calificación ──────────────────────────────────────────────────────
    /**
     * Calificación según escala vigesimal (0–20) para Secundaria,
     * o escala AD/A/B/C para Primaria.
     */
    @Column(name = "calificacion_numerica", precision = 4, scale = 1)
    private BigDecimal calificacionNumerica;

    @Enumerated(EnumType.STRING)
    @Column(name = "calificacion_literal", length = 5)
    private CalificacionLiteral calificacionLiteral; // Para primaria

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    // ── Evidencias ────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "nota", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Evidencia> evidencias = new ArrayList<>();

    // ── Auditoría ─────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Enums ─────────────────────────────────────────────────────────────
    public enum CalificacionLiteral {
        AD,  // Logro destacado (18–20)
        A,   // Logro previsto  (14–17)
        B,   // En proceso      (11–13)
        C    // En inicio       (0–10)
    }

    /** Retorna la calificación literal calculada desde la numérica. */
    public CalificacionLiteral calcularLiteral() {
        if (calificacionNumerica == null) return null;
        int valor = calificacionNumerica.intValue();
        if (valor >= 18) return CalificacionLiteral.AD;
        if (valor >= 14) return CalificacionLiteral.A;
        if (valor >= 11) return CalificacionLiteral.B;
        return CalificacionLiteral.C;
    }
}
