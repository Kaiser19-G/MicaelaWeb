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
    private String aulaReferencia;
    private String descripcion;     // "5to A – SECUNDARIA (2026)"

    // ── Docente Principal ─────────────────────────────────────────────────────
    private Long docentePrincipalId;
    private String docentePrincipalNombre;

    /** Total de alumnos activos asignados al aula */
    private int totalAlumnos;

    public static AulaResponseDTO fromEntity(Aula a) {
        return AulaResponseDTO.builder()
            .id(a.getId())
            .grado(a.getGrado())
            .seccion(a.getSeccion())
            .nivel(a.getNivel() != null ? a.getNivel().name() : null)
            .anioAcademico(a.getAnioAcademico())
            .aulaReferencia(a.getAulaReferencia())
            .descripcion(a.getDescripcion())
            .docentePrincipalId(a.getDocentePrincipal() != null
                ? a.getDocentePrincipal().getId() : null)
            .docentePrincipalNombre(a.getDocentePrincipal() != null
                ? a.getDocentePrincipal().getNombreCompleto() : null)
            .totalAlumnos(a.getAlumnos() != null ? a.getAlumnos().size() : 0)
            .build();
    }
}
