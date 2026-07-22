package com.colegio.bastidas.dto.aula;

import com.colegio.bastidas.dto.dashboard.DashboardKpiDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Resumen de asistencia de una sola aula para el período elegido
 * (día/semana/mes/año) — alimenta el dashboard dedicado del módulo Aulas.
 */
@Data
@Builder
public class AulaResumenAsistenciaDTO {

    private Long aulaId;
    private String aulaDescripcion;
    private String tipoPeriodo;

    private long totalAlumnos;
    private long presentesEnPeriodo;
    private long faltasEnPeriodo;
    private long tardanzasEnPeriodo;

    /** Serie para el gráfico de barras, ya bucketizada según el período. */
    private List<DashboardKpiDTO.AsistenciaDiaDTO> buckets;
}
