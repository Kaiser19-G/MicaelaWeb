package com.colegio.bastidas.controller;

import com.colegio.bastidas.dto.dashboard.DashboardKpiDTO;
import com.colegio.bastidas.model.Asistencia;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.AsistenciaRepository;
import com.colegio.bastidas.repository.DocenteRepository;
import com.colegio.bastidas.service.AulaService;
import com.colegio.bastidas.service.DocenteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controlador del Dashboard del Director.
 * Base: {@code /api/v1/dashboard}
 *
 * Agrega todos los KPIs e indicadores estratégicos en un solo endpoint
 * para minimizar las llamadas HTTP desde el frontend (< 2.5 seg RNF-03).
 */
@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final AlumnoRepository alumnoRepository;
    private final DocenteRepository docenteRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final DocenteService docenteService;
    private final AulaService aulaService;

    /**
     * GET /dashboard/kpis?anio=2026
     * Retorna todos los KPIs del Panel del Director en una sola llamada.
     */
    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<DashboardKpiDTO> obtenerKpis(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") Integer anio) {

        LocalDate hoy = LocalDate.now();
        int anioActual = anio != null ? anio : Year.now().getValue();

        // ── KPIs principales ──────────────────────────────────────────────────
        long alumnosTotales  = alumnoRepository.contarAlumnosActivosPorAnio(anioActual);
        long docentesTotales = docenteRepository.count();
        long aulasTotales    = aulaService.contarPorAnio(anioActual);

        // ── Asistencia del día ────────────────────────────────────────────────
        long presentesHoy = asistenciaRepository
            .countByFechaAndEstado(hoy, Asistencia.EstadoAsistencia.ASISTIO);
        long faltasHoy = asistenciaRepository
            .countByFechaAndEstado(hoy, Asistencia.EstadoAsistencia.FALTA);

        // ── Semáforo curricular ───────────────────────────────────────────────
        long aprobados  = docenteService.contarPorEstadoCurricular("APROBADO");
        long pendientes = docenteService.contarPorEstadoCurricular("PENDIENTE");
        long retrasados = docenteService.contarPorEstadoCurricular("RETRASADO");

        // ── Asistencia semanal (últimos 5 días hábiles) ───────────────────────
        List<DashboardKpiDTO.AsistenciaDiaDTO> semanal = construirAsistenciaSemanal(hoy);

        DashboardKpiDTO kpis = DashboardKpiDTO.builder()
            .alumnosTotales(alumnosTotales)
            .docentesTotales(docentesTotales)
            .aulasTotales(aulasTotales)
            .alumnosPresentesHoy(presentesHoy)
            .alumnosFaltasHoy(faltasHoy)
            .alumnosConPermisoAcademia(0L) // se calcula en Sprint 5
            .docentesAprobados(aprobados)
            .docentesPendientes(pendientes)
            .docentesRetrasados(retrasados)
            .alumnosMatriculadosCompletos(alumnosTotales)
            .alumnosMatriculaProvisional(0L)
            .asistenciaSemanal(semanal)
            .build();

        return ResponseEntity.ok(kpis);
    }

    /**
     * GET /dashboard/alertas
     * Retorna las alertas activas (servicios básicos, documentos faltantes, etc.)
     */
    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerAlertas() {
        // En Sprint 5 se conectará con tabla de alertas reales
        List<Map<String, Object>> alertas = List.of(
            Map.of("tipo", "DOCUMENTOS", "titulo", "Documentos Faltantes",
                   "subtitulo", "DNI/Partidas sin entregar", "cantidad", 0),
            Map.of("tipo", "MATRICULA", "titulo", "Matrículas Provisionales",
                   "subtitulo", "Pendientes de documentación completa", "cantidad", 0)
        );
        return ResponseEntity.ok(alertas);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Construye los datos de asistencia de los últimos 5 días hábiles
     * para el gráfico de barras del dashboard.
     */
    private List<DashboardKpiDTO.AsistenciaDiaDTO> construirAsistenciaSemanal(LocalDate hoy) {
        String[] dias = {"Lun", "Mar", "Mié", "Jue", "Vie"};
        List<DashboardKpiDTO.AsistenciaDiaDTO> resultado = new ArrayList<>();

        // Obtener el lunes de la semana actual
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);

        for (int i = 0; i < 5; i++) {
            LocalDate dia = lunes.plusDays(i);
            long presentes = asistenciaRepository
                .countByFechaAndEstado(dia, Asistencia.EstadoAsistencia.ASISTIO);

            resultado.add(DashboardKpiDTO.AsistenciaDiaDTO.builder()
                .dia(dias[i])
                .alumnos(presentes)
                .docentes(0L) // Se expandirá cuando exista asistencia de docentes
                .build());
        }
        return resultado;
    }
}
