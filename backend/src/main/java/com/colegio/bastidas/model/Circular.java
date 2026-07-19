package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Comunicado oficial del Director. Se crea como borrador y se publica cuando
 * está lista; no hay envío por correo/SMS — es un tablón de anuncios visible
 * en el sistema para el público objetivo indicado.
 */
@Entity
@Table(name = "circulares")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Circular {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    @NotBlank
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(name = "dirigido_a", nullable = false, length = 20)
    @NotNull
    private DirigidoA dirigidoA;

    @Column(nullable = false)
    @Builder.Default
    private Boolean publicada = false;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicada_por_id", nullable = false)
    @NotNull
    private Usuario publicadaPor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum DirigidoA {
        TODOS, DOCENTES, ALUMNOS
    }
}
