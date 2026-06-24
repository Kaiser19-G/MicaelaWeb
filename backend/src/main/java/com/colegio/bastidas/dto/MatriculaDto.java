package com.colegio.bastidas.dto;

import com.colegio.bastidas.model.Matricula.EstadoMatricula;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatriculaDto {
    private Long id;

    @NotNull(message = "El ID del alumno es obligatorio")
    private Long alumnoId;

    private String nombreAlumno; // Solo para lectura
    private String codigoAlumno; // Solo para lectura

    @NotBlank(message = "El grado es obligatorio")
    private String grado;

    @NotBlank(message = "La seccion es obligatoria")
    private String seccion;

    @NotNull(message = "El año escolar es obligatorio")
    @Min(2000)
    private Integer anioEscolar;

    private EstadoMatricula estado;
}
