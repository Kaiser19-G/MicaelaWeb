package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Aula – representa una sección académica (ej. "5to A Secundaria").
 */
@Entity
@Table(name = "aulas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"alumnos"})
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    @NotBlank
    private String grado; // "1ro", "2do", "3ro", "4to", "5to", "6to"

    @Column(nullable = false, length = 5)
    @NotBlank
    private String seccion; // "A", "B", "C", "D"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @NotNull
    private Nivel nivel; // PRIMARIA / SECUNDARIA

    @Column(name = "anio_academico", nullable = false)
    private Integer anioAcademico;

    @Column(nullable = false)
    @NotNull
    @Min(1)
    private Integer capacidad;

    @Column(name = "aula_referencia", length = 20)
    private String aulaReferencia; // "Pabellón A – Aula 201"

    @ManyToOne(fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @JoinColumn(name = "docente_principal_id")
    private Docente docentePrincipal;

    @OneToMany(mappedBy = "aula", fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private List<Alumno> alumnos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Nivel { PRIMARIA, SECUNDARIA }

    public String getDescripcion() {
        return String.format("%s %s – %s (%d)", grado, seccion, nivel, anioAcademico);
    }
}
