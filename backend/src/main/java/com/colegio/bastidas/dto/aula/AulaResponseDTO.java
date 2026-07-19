package com.colegio.bastidas.dto.aula;

import com.colegio.bastidas.model.Aula;
import lombok.Builder;
import lombok.Data;

/**
 * DTO de respuesta para Aula.
 */
@Data
@Builder
public class AulaResponseDTO {

    private Long id;
    private String grado;
    private String seccion;
    private String nivel;           // "PRIMARIA" | "SECUNDARIA"
    private Integer anioAcademico;
    private Integer capacidad;
    private String aulaReferencia;
    private String descripcion;     // "5to A – SECUNDARIA (2026)"

    // ── Docente Principal ─────────────────────────────────────────────────────
    private Long docentePrincipalId;
    private String docentePrincipalNombre;

    /** Total de alumnos activos asignados al aula (calculado por el servicio). */
    private int totalAlumnos;

    /** Cupos libres = capacidad - totalAlumnos (calculado por el servicio). */
    private int vacantesDisponibles;

    /** Construye el DTO sin datos de ocupación (usar {@link #fromEntity(Aula, int)} cuando se conozcan). */
    public static AulaResponseDTO fromEntity(Aula a) {
        return fromEntity(a, 0);
    }

    public static AulaResponseDTO fromEntity(Aula a, int totalAlumnosActivos) {
        int capacidad = a.getCapacidad() != null ? a.getCapacidad() : 0;
        return AulaResponseDTO.builder()
            .id(a.getId())
            .grado(a.getGrado())
            .seccion(a.getSeccion())
            .nivel(a.getNivel() != null ? a.getNivel().name() : null)
            .anioAcademico(a.getAnioAcademico())
            .capacidad(a.getCapacidad())
            .aulaReferencia(a.getAulaReferencia())
            .descripcion(a.getDescripcion())
            .docentePrincipalId(a.getDocentePrincipal() != null
                ? a.getDocentePrincipal().getId() : null)
            .docentePrincipalNombre(a.getDocentePrincipal() != null
                ? a.getDocentePrincipal().getNombreCompleto() : null)
            .totalAlumnos(totalAlumnosActivos)
            .vacantesDisponibles(Math.max(0, capacidad - totalAlumnosActivos))
            .build();
    }
}
