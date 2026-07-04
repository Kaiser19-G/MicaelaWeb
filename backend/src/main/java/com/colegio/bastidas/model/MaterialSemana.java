package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "materiales_semana", indexes = {
    @Index(name = "idx_material_curso", columnList = "curso_asignado_id"),
    @Index(name = "idx_material_semana", columnList = "curso_asignado_id, semana")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class MaterialSemana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @JoinColumn(name = "curso_asignado_id", nullable = false)
    @NotNull
    private CursoAsignado cursoAsignado;

    @Column(nullable = false)
    @NotNull
    private Integer semana;

    @Column(name = "nombre_archivo", nullable = false, length = 200)
    @NotBlank
    private String nombreArchivo;

    @Column(name = "url_archivo", nullable = false, length = 1000)
    @NotBlank
    private String urlArchivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @JoinColumn(name = "docente_id", nullable = false)
    @NotNull
    private Docente docente;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
