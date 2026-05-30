package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "entregas_tarea", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tarea_id", "alumno_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class EntregaTarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_id", nullable = false)
    private Tarea tarea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Column(name = "archivo_url", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String archivoUrl;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    @NotBlank
    private String nombreArchivo;

    @CreationTimestamp
    @Column(name = "fecha_entrega", updatable = false)
    private LocalDateTime fechaEntrega;

    @Column(name = "nota_asignada", precision = 4, scale = 2)
    private BigDecimal notaAsignada;

    @Column(name = "comentario_docente", columnDefinition = "TEXT")
    private String comentarioDocente;
}
