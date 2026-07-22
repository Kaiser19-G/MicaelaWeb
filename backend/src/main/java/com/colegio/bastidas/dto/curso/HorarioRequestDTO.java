package com.colegio.bastidas.dto.curso;

import com.colegio.bastidas.model.CursoAsignado;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

/**
 * DTO de entrada para fijar/editar el horario (día + hora) de una
 * asignación de curso ya creada.
 */
@Data
public class HorarioRequestDTO {

    @NotNull(message = "El día de la semana es obligatorio")
    private CursoAsignado.DiaSemana diaSemana;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;
}
