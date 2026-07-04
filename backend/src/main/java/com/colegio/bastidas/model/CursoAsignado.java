package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad intermedia que vincula a un Docente con un Aula para impartir una materia específica
 * en un año académico determinado.
 */
@Entity
@Table(name = "cursos_asignados", indexes = {
    @Index(name = "idx_curso_docente", columnList = "docente_id"),
    @Index(name = "idx_curso_aula", columnList = "aula_id")
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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
