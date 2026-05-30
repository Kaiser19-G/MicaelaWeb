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
 * Entidad que representa a un Docente de la I.E. Micaela Bastidas.
 * Capacidad institucional: 68 docentes.
 */
@Entity
@Table(name = "docentes", indexes = {
    @Index(name = "idx_docente_dni", columnList = "dni", unique = true),
    @Index(name = "idx_docente_codigo", columnList = "codigo_docente", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"aulas", "asistenciasRegistradas"})
public class Docente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_docente", nullable = false, unique = true, length = 12)
    @NotBlank
    private String codigoDocente;

    @Column(nullable = false, length = 8, unique = true)
    @NotBlank
    @Size(min = 8, max = 8)
    private String dni;

    @Column(name = "apellido_paterno", nullable = false, length = 60)
    @NotBlank
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false, length = 60)
    @NotBlank
    private String apellidoMaterno;

    @Column(nullable = false, length = 100)
    @NotBlank
    private String nombres;

    @Column(name = "especialidad", length = 100)
    private String especialidad; // Ej: "Matemática", "Comunicación"

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Condicion condicion = Condicion.NOMBRADO;

    @Column(name = "email_institucional", length = 100)
    private String emailInstitucional;

    @Column(length = 15)
    private String celular;

    // ── Relación con Usuario ──────────────────────────────────────────────
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // ── Relaciones ────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "docentePrincipal", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Aula> aulas = new ArrayList<>();

    @OneToMany(mappedBy = "docente", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Asistencia> asistenciasRegistradas = new ArrayList<>();



    // ── Auditoría ─────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Condicion { NOMBRADO, CONTRATADO }

    public String getNombreCompleto() {
        return String.format("%s %s, %s", apellidoPaterno, apellidoMaterno, nombres);
    }
}
