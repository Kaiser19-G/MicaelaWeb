package com.colegio.bastidas.dto.calidad;

import lombok.Builder;
import lombok.Data;

/**
 * Validación de un aula antes de exportar al SIAGIE: cuántos alumnos tienen
 * al menos una nota registrada este año, y cuántas notas fueron registradas
 * por un docente que no tiene ese curso asignado en esa aula (inconsistencia).
 */
@Data
@Builder
public class AulaValidacionDTO {

    private Long aulaId;
    private String nombre;
    private String estado; // "ok" | "advertencia" | "error"
    private int estudiantes;
    private int notasCompletas;   // alumnos con >=1 nota registrada
    private int notasBlanco;      // alumnos sin ninguna nota registrada
    private int inconsistencias;  // notas de un docente no asignado a esa área en esa aula
}
