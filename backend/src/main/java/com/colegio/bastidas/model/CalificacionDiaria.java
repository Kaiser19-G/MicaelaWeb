package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "calificaciones_diarias", indexes = {
    @Index(name = "idx_calif_diaria_alumno", columnList = "alumno_id"),
    @Index(name = "idx_calif_diaria_semana", columnList = "curso_asignado_id, semana")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CalificacionDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    @NotNull
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    @NotNull
    private Docente docente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_asignado_id", nullable = false)
    @NotNull
    private CursoAsignado cursoAsignado;

    @Column(nullable = false)
    @NotNull
    private Integer semana;

    @Column(nullable = false, length = 2)
    @NotBlank
    private String calificacion; // "AD", "A", "B", "C", "D", "NP"

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
