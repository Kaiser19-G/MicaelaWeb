package com.colegio.bastidas.dto.curso;

import com.colegio.bastidas.model.CursoAsignado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

/**
 * DTO de entrada para asignar un docente a un aula + área curricular
 * (Director/Admin). El horario es opcional: si se envía uno de los tres
 * campos, deben venir los tres juntos.
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

    private CursoAsignado.DiaSemana diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}
