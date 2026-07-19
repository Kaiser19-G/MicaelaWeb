package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reunión con el apoderado de un alumno, agendada por el Director o por el
 * Docente-Tutor del aula (solo Secundaria). El mensaje de WhatsApp al
 * apoderado se genera como un link {@code wa.me} a partir de estos datos —
 * no requiere un número de WhatsApp propio de la institución.
 */
@Entity
@Table(name = "reuniones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Reunion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    @NotNull
    private Alumno alumno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aula_id", nullable = false)
    @NotNull
    private Aula aula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "convocada_por_id", nullable = false)
    @NotNull
    private Usuario convocadaPor;

    @Column(nullable = false)
    @NotNull
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false, length = 5)
    @NotBlank
    private String horaInicio;

    @Column(name = "hora_fin", nullable = false, length = 5)
    @NotBlank
    private String horaFin;

    @Column(nullable = false, length = 300)
    @NotBlank
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Estado estado = Estado.PENDIENTE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Estado {
        PENDIENTE, CONFIRMADA, REALIZADA, CANCELADA
    }
}
