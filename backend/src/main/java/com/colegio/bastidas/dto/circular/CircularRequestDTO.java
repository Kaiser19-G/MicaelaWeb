package com.colegio.bastidas.dto.circular;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CircularRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    @NotBlank(message = "El contenido es obligatorio")
    private String contenido;

    @NotNull(message = "Debe indicar a quién va dirigido")
    private String dirigidoA; // "TODOS" | "DOCENTES" | "ALUMNOS"
}
