package com.colegio.bastidas.dto.curso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de entrada para asignar un docente a un aula + área curricular
 * (Director/Admin).
 */
@Data
public class CursoAsignadoRequestDTO {

    @NotNull(message = "El docente es obligatorio")
    private Long docenteId;

    @NotNull(message = "El aula es obligatoria")
    private Long aulaId;

    @NotBlank(message = "El área curricular es obligatoria")
    private String areaCurricular;

    @NotNull(message = "El año académico es obligatorio")
    private Integer anioAcademico;
}
