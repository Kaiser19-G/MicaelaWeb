package com.colegio.bastidas.dto.docente;

import lombok.Builder;
import lombok.Data;

/**
 * Respuesta de {@code POST /docentes}: incluye las credenciales generadas
 * automáticamente para que el Director las entregue al docente.
 * Se muestran una única vez; no se vuelven a exponer en ningún otro endpoint.
 */
@Data
@Builder
public class DocenteCreadoResponseDTO {
    private DocenteResponseDTO docente;
    private String usernameGenerado;
    private String passwordInicial;
}
