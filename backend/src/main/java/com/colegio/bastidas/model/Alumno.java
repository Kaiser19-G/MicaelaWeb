package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa a un alumno matriculado en la I.E. Micaela Bastidas.
 * Capacidad institucional: ~1500 alumnos.
 */
@Entity
@Table(name = "alumnos", indexes = {
    @Index(name = "idx_alumno_dni", columnList = "dni", unique = true),
    @Index(name = "idx_alumno_codigo", columnList = "codigo_estudiante", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"asistencias", "notas", "expedienteDocumentos"})
public class Alumno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Datos Personales ──────────────────────────────────────────────────
    @Column(name = "codigo_estudiante", nullable = false, unique = true, length = 12)
    @NotBlank
    private String codigoEstudiante;

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

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sexo sexo;

    // ── Datos de Matrícula ────────────────────────────────────────────────
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aula_id")
    private Aula aula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id")
    private Docente tutor;

    @Column(name = "anio_academico")
    @Min(2020) @Max(2100)
    private Integer anioAcademico;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_matricula", length = 20)
    @Builder.Default
    private EstadoMatricula estadoMatricula = EstadoMatricula.ACTIVO;

    // ── Permiso de Academia ───────────────────────────────────────────────
    /**
     * Indica si el alumno tiene permiso de academia pre-universitaria.
     * Impacta la lógica de asistencia: entrada permitida 1:30 PM o 2:30 PM.
     */
    @Column(name = "tiene_permiso_academia")
    @Builder.Default
    private Boolean tienePermisoAcademia = false;

    @Column(name = "hora_entrada_academia", length = 10)
    private String horaEntradaAcademia; // Ej: "13:30" o "14:30"

    // ── Contacto del Apoderado ────────────────────────────────────────────
    @Column(name = "nombre_apoderado", length = 150)
    private String nombreApoderado;

    @Column(name = "celular_apoderado", length = 15)
    private String celularApoderado;

    @Column(name = "email_apoderado", length = 100)
    @Email
    private String emailApoderado;

    // ── Relaciones ────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Asistencia> asistencias = new ArrayList<>();

    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Nota> notas = new ArrayList<>();

    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpedienteDocumento> expedienteDocumentos = new ArrayList<>();

    // ── Auditoría ─────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Enums Internos ────────────────────────────────────────────────────
    public enum Sexo { MASCULINO, FEMENINO }

    public enum EstadoMatricula { ACTIVO, RETIRADO, TRASLADADO, EGRESADO }

    // ── Métodos de Negocio ────────────────────────────────────────────────
    /** Retorna el nombre completo en formato "APELLIDOS, Nombres". */
    public String getNombreCompleto() {
        return String.format("%s %s, %s", apellidoPaterno, apellidoMaterno, nombres);
    }
}
