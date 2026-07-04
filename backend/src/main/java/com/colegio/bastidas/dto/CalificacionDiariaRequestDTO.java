package com.colegio.bastidas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CalificacionDiariaRequestDTO {
    @NotNull
    private Long alumnoId;
    
    @NotBlank
    private String calificacion;
}
