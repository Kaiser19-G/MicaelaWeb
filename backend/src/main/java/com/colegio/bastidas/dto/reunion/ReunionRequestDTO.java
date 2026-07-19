package com.colegio.bastidas.dto.reunion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO de petición para agendar una reunión con el apoderado.
 * Para una reunión general de aula se usa {@code POST /reuniones/aula/{aulaId}},
 * que reutiliza los mismos fecha/horaInicio/horaFin/motivo para cada alumno activo.
 */
@Data
public class ReunionRequestDTO {

    private Long alumnoId; // requerido solo para la reunión individual (POST /reuniones)

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotBlank(message = "La hora de inicio es obligatoria")
    private String horaInicio;

    @NotBlank(message = "La hora de fin es obligatoria")
    private String horaFin;

    @NotBlank(message = "El motivo es obligatorio")
    private String motivo;
}
