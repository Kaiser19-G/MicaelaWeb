package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidad intermedia que vincula a un Docente con un Aula para impartir una materia específica
 * en un año académico determinado.
 */
@Entity
@Table(name = "cursos_asignados", indexes = {
    @Index(name = "idx_curso_docente", columnList = "docente_id"),
    @Index(name = "idx_curso_aula", columnList = "aula_id"),
    @Index(name = "idx_curso_aula_dia", columnList = "aula_id,dia_semana"),
    @Index(name = "idx_curso_docente_dia", columnList = "docente_id,dia_semana")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CursoAsignado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    @NotNull
    private Docente docente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aula_id", nullable = false)
    @NotNull
    private Aula aula;

    @Column(name = "area_curricular", nullable = false, length = 100)
    @NotBlank
    private String areaCurricular; // Ej: "Matemática", "Comunicación"

    @Column(name = "anio_academico", nullable = false)
    @NotNull
    private Integer anioAcademico; // Ej: 2026

    // ── Horario (opcional: se completa después de crear la asignación,
    //    p. ej. el flujo de tutor de Primaria crea varias filas de golpe sin horario individual) ──
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", length = 12)
    private DiaSemana diaSemana;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DiaSemana {
        LUNES, MARTES, MIERCOLES, JUEVES, VIERNES
    }
}
