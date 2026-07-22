package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.dashboard.DashboardKpiDTO;
import com.colegio.bastidas.dto.dashboard.PeriodoResumen;

import java.util.List;
import java.util.Map;

/**
 * KPIs y reportes del Panel del Director.
 */
public interface DashboardService {

    DashboardKpiDTO obtenerKpis(Integer anio);

    /** Resumen de asistencia (alumnos ASISTIO) agrupado según el período elegido: SEMANA/MES/ANIO. */
    List<DashboardKpiDTO.AsistenciaDiaDTO> obtenerAsistenciaPorPeriodo(PeriodoResumen periodo, Integer anio, Integer mes);

    List<Map<String, Object>> obtenerAlertas(Integer anio);

    byte[] exportarResumenExcel(Integer anio);

    byte[] exportarResumenPdf(Integer anio);
}
