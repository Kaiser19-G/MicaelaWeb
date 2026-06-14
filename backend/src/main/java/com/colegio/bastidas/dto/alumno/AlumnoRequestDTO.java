package com.colegio.bastidas.dto.alumno;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de petición para crear o actualizar un Alumno.
 */
@Data
public class AlumnoRequestDTO {

    @NotBlank(message = "El código de estudiante es obligatorio")
    @Size(max = 12)
    private String codigoEstudiante;

    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 8, max = 8, message = "El DNI debe tener exactamente 8 dígitos")
    private String dni;

    @NotBlank(message = "El apellido paterno es obligatorio")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    private String apellidoMaterno;

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    private String fechaNacimiento; // "YYYY-MM-DD"
    private String sexo;            // "MASCULINO" | "FEMENINO"

    @NotNull(message = "El año académico es obligatorio")
    @Min(2020) @Max(2100)
    private Integer anioAcademico;

    private Long aulaId;

    // ── Permiso Academia ─────────────────────────────────────────────────────
    private Boolean tienePermisoAcademia = false;
    private String horaEntradaAcademia;

    // ── Apoderado ────────────────────────────────────────────────────────────
    private String nombreApoderado;
    private String celularApoderado;

    @Email(message = "Email del apoderado no tiene formato válido")
    private String emailApoderado;
}
