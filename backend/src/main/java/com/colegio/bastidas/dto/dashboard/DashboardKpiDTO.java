package com.colegio.bastidas.dto.dashboard;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * DTO de respuesta para el Dashboard del Director.
 * Agrupa todos los KPIs y estadísticas necesarios para la vista del Panel.
 */
@Data
@Builder
public class DashboardKpiDTO {

    // ── KPIs Principales ─────────────────────────────────────────────────────
    /** Total de alumnos activos en el año académico actual */
    private long alumnosTotales;
    /** Total de docentes registrados */
    private long docentesTotales;
    /** Total de aulas registradas en el año actual */
    private long aulasTotales;

    // ── Asistencia del Día ────────────────────────────────────────────────────
    private long alumnosPresentesHoy;
    private long alumnosFaltasHoy;
    private long alumnosConPermisoAcademia;

    // ── Semáforo Curricular ───────────────────────────────────────────────────
    private long docentesAprobados;
    private long docentesPendientes;
    private long docentesRetrasados;

    // ── Matrículas ────────────────────────────────────────────────────────────
    private long alumnosMatriculadosCompletos;
    private long alumnosMatriculaProvisional; // Faltan documentos

    // ── Asistencia Semanal (para el gráfico de barras) ───────────────────────
    private List<AsistenciaDiaDTO> asistenciaSemanal;

    @Data
    @Builder
    public static class AsistenciaDiaDTO {
        private String dia;          // "Lun", "Mar", ...
        private long alumnos;
        private long docentes;
    }
}
