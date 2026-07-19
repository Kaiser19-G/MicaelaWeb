package com.colegio.bastidas.dto.alumno;

import lombok.Builder;
import lombok.Data;

/**
 * Respuesta de {@code POST /alumnos}: incluye las credenciales generadas
 * automáticamente para que el Director las entregue al alumno/apoderado.
 * Se muestran una única vez; no se vuelven a exponer en ningún otro endpoint.
 */
@Data
@Builder
public class AlumnoCreadoResponseDTO {
    private AlumnoResponseDTO alumno;
    private String usernameGenerado;
    private String passwordInicial;
}
