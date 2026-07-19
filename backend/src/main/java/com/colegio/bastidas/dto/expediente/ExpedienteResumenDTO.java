package com.colegio.bastidas.dto.expediente;

import lombok.Builder;
import lombok.Data;

/**
 * Resumen del expediente digital de un alumno para la tabla del Panel de Dirección.
 * Evita el N+1: se construye a partir de una sola consulta agregada por año.
 */
@Data
@Builder
public class ExpedienteResumenDTO {

    private Long alumnoId;
    private String nombreCompleto;
    private String dni;
    private String aulaDescripcion;
    private boolean tieneDni;
    private boolean tienePartidaNacimiento;
    private int totalDocumentos;

    public boolean isCompleto() {
        return tieneDni && tienePartidaNacimiento;
    }
}
