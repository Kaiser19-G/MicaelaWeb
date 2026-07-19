package com.colegio.bastidas.dto;

import com.colegio.bastidas.model.Matricula.EstadoMatricula;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatriculaDto {
    private Long id;

    @NotNull(message = "El ID del alumno es obligatorio")
    private Long alumnoId;

    private String nombreAlumno; // Solo para lectura
    private String codigoAlumno; // Solo para lectura

    @NotNull(message = "El aula es obligatoria")
    private Long aulaId;

    private String grado;   // Solo para lectura (derivado del aula)
    private String seccion; // Solo para lectura (derivado del aula)

    @NotNull(message = "El año escolar es obligatorio")
    @Min(2000)
    private Integer anioEscolar;

    private EstadoMatricula estado;
}
