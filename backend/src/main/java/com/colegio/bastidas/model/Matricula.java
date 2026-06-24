package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "matriculas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"alumno_id", "anio_escolar"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    @NotNull
    private Alumno alumno;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String grado;

    @NotBlank
    @Column(nullable = false, length = 10)
    private String seccion;

    @NotNull
    @Min(2000)
    @Column(name = "anio_escolar", nullable = false)
    private Integer anioEscolar;

    @CreationTimestamp
    @Column(name = "fecha_matricula", updatable = false)
    private LocalDateTime fechaMatricula;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private EstadoMatricula estado = EstadoMatricula.ACTIVO;

    public enum EstadoMatricula {
        ACTIVO, RETIRADO, TRASLADADO
    }
}
