package com.colegio.bastidas.dto.docente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO de petición para crear o actualizar un Docente.
 */
@Data
public class DocenteRequestDTO {

    @NotBlank(message = "El código de docente es obligatorio")
    @Size(max = 12)
    private String codigoDocente;

    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 8, max = 8, message = "El DNI debe tener exactamente 8 dígitos")
    private String dni;

    @NotBlank(message = "El apellido paterno es obligatorio")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    private String apellidoMaterno;

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    private String especialidad;

    /** "NOMBRADO" o "CONTRATADO" */
    private String condicion;

    @Email(message = "El email no tiene formato válido")
    private String emailInstitucional;

    private String celular;
}
