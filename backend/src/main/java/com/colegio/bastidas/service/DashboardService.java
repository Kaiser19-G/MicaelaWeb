package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.dashboard.DashboardKpiDTO;
import com.colegio.bastidas.dto.dashboard.PeriodoResumen;

import java.util.List;
import java.util.Map;

/**
 * KPIs y reportes del Panel del Director.
 */
public interface DashboardService {

    /**
     * KPIs del Panel del Director. {@code anio} sigue acotando lo estructural (alumnos/docentes/
     * aulas matriculados ese año académico); {@code periodo}/{@code mes} determinan sobre qué rango
     * de fechas se calculan las métricas de asistencia (presentes/faltas/gráfico) — SEMANA (actual),
     * MES (elegido) o ANIO (elegido).
     */
    DashboardKpiDTO obtenerKpis(Integer anio, PeriodoResumen periodo, Integer mes);

    List<Map<String, Object>> obtenerAlertas(Integer anio);

    /** Exporta el mismo resumen que ve el Director, ya filtrado por el período elegido. */
    byte[] exportarResumenExcel(Integer anio, PeriodoResumen periodo, Integer mes);

    byte[] exportarResumenPdf(Integer anio, PeriodoResumen periodo, Integer mes);
}
