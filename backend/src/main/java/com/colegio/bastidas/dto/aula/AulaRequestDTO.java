package com.colegio.bastidas.dto.aula;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de entrada para crear un Aula (Director/Admin).
 */
@Data
public class AulaRequestDTO {

    @NotBlank
    private String grado;

    @NotBlank
    private String seccion;

    @NotBlank
    private String nivel; // "PRIMARIA" | "SECUNDARIA"

    @NotNull
    private Integer anioAcademico;

    @NotNull
    @Min(1)
    private Integer capacidad;

    private String aulaReferencia;

    private Long docentePrincipalId;
}
