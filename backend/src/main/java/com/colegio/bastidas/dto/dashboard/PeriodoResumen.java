package com.colegio.bastidas.dto.dashboard;

/**
 * Granularidad del resumen de asistencia (Dashboard del Director y módulo Aulas).
 * DIA solo lo usa el dashboard de una aula individual; el panel del Director
 * expone SEMANA/MES/ANIO.
 */
public enum PeriodoResumen {
    DIA,
    SEMANA,
    MES,
    ANIO
}
