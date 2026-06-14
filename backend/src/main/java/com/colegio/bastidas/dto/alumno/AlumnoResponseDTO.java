package com.colegio.bastidas.dto.alumno;

import com.colegio.bastidas.model.Alumno;
import lombok.Builder;
import lombok.Data;

/**
 * DTO de respuesta para Alumno.
 * Solo expone lo necesario para el frontend; no expone la entidad JPA.
 */
@Data
@Builder
public class AlumnoResponseDTO {

    private Long id;
    private String codigoEstudiante;
    private String dni;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombres;
    private String nombreCompleto;   // "APELLIDOS, Nombres"
    private String fechaNacimiento;
    private String sexo;

    // ── Matrícula ────────────────────────────────────────────────────────────
    private String estadoMatricula;  // "ACTIVO" | "RETIRADO" | etc.
    private Integer anioAcademico;

    // ── Aula ─────────────────────────────────────────────────────────────────
    private Long aulaId;
    private String aulaDescripcion;  // "5to A – SECUNDARIA (2026)"

    // ── Permiso Academia ─────────────────────────────────────────────────────
    private Boolean tienePermisoAcademia;
    private String horaEntradaAcademia; // "13:30" | "14:30"

    // ── Apoderado ────────────────────────────────────────────────────────────
    private String nombreApoderado;
    private String celularApoderado;
    private String emailApoderado;

    public static AlumnoResponseDTO fromEntity(Alumno a) {
        return AlumnoResponseDTO.builder()
            .id(a.getId())
            .codigoEstudiante(a.getCodigoEstudiante())
            .dni(a.getDni())
            .apellidoPaterno(a.getApellidoPaterno())
            .apellidoMaterno(a.getApellidoMaterno())
            .nombres(a.getNombres())
            .nombreCompleto(a.getNombreCompleto())
            .fechaNacimiento(a.getFechaNacimiento() != null
                ? a.getFechaNacimiento().toString() : null)
            .sexo(a.getSexo() != null ? a.getSexo().name() : null)
            .estadoMatricula(a.getEstadoMatricula() != null
                ? a.getEstadoMatricula().name() : null)
            .anioAcademico(a.getAnioAcademico())
            .aulaId(a.getAula() != null ? a.getAula().getId() : null)
            .aulaDescripcion(a.getAula() != null ? a.getAula().getDescripcion() : null)
            .tienePermisoAcademia(Boolean.TRUE.equals(a.getTienePermisoAcademia()))
            .horaEntradaAcademia(a.getHoraEntradaAcademia())
            .nombreApoderado(a.getNombreApoderado())
            .celularApoderado(a.getCelularApoderado())
            .emailApoderado(a.getEmailApoderado())
            .build();
    }
}
