package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.dto.dashboard.DashboardKpiDTO;
import com.colegio.bastidas.dto.dashboard.PeriodoResumen;
import com.colegio.bastidas.model.Asistencia;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.AsistenciaRepository;
import com.colegio.bastidas.repository.DocenteRepository;
import com.colegio.bastidas.service.AulaService;
import com.colegio.bastidas.service.DashboardService;
import com.colegio.bastidas.service.DocenteService;
import com.colegio.bastidas.service.ExcelReportService;
import com.colegio.bastidas.service.MatriculaService;
import com.colegio.bastidas.service.PdfReportService;
import com.colegio.bastidas.service.support.PeriodoBucketHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final AlumnoRepository alumnoRepository;
    private final DocenteRepository docenteRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final DocenteService docenteService;
    private final AulaService aulaService;
    private final MatriculaService matriculaService;
    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;

    @Override
    public DashboardKpiDTO obtenerKpis(Integer anio, PeriodoResumen periodo, Integer mes) {
        LocalDate hoy = LocalDate.now();
        int anioActual = anio != null ? anio : Year.now().getValue();
        PeriodoResumen tipo = periodo != null ? periodo : PeriodoResumen.SEMANA;

        // ── Estructural: alumnos/docentes/aulas matriculados en el año académico ──
        long alumnosTotales  = alumnoRepository.contarAlumnosActivosPorAnio(anioActual);
        long docentesTotales = docenteRepository.count();
        long aulasTotales    = aulaService.contarPorAnio(anioActual);

        // ── Asistencia del período elegido (semana actual / mes elegido / año elegido) ──
        List<PeriodoBucketHelper.Bucket> buckets = PeriodoBucketHelper.construirBuckets(tipo, hoy, anio, mes);
        LocalDate inicioPeriodo = buckets.get(0).inicio();
        LocalDate finPeriodo = buckets.get(buckets.size() - 1).fin();

        long presentesPeriodo = asistenciaRepository.countByEstadoAndFechaBetween(
            Asistencia.EstadoAsistencia.ASISTIO, inicioPeriodo, finPeriodo);
        long faltasPeriodo = asistenciaRepository.countByEstadoAndFechaBetween(
            Asistencia.EstadoAsistencia.FALTA, inicioPeriodo, finPeriodo);

        List<Object[]> filasFecha = asistenciaRepository.countAsistioPorFechaEntre(null, inicioPeriodo, finPeriodo);
        List<DashboardKpiDTO.AsistenciaDiaDTO> asistenciaPeriodo = PeriodoBucketHelper.aplicarConteos(buckets, filasFecha);

        // ── Semáforo curricular ────────────────────────────────────────────────
        long aprobados  = docenteService.contarPorEstadoCurricular("APROBADO");
        long pendientes = docenteService.contarPorEstadoCurricular("PENDIENTE");
        long retrasados = docenteService.contarPorEstadoCurricular("RETRASADO");

        // ── Expedientes incompletos (matrícula "provisional") ─────────────────
        long expedientesIncompletos = matriculaService.contarConExpedienteIncompleto(anioActual);

        return DashboardKpiDTO.builder()
            .alumnosTotales(alumnosTotales)
            .docentesTotales(docentesTotales)
            .aulasTotales(aulasTotales)
            .alumnosPresentesPeriodo(presentesPeriodo)
            .alumnosFaltasPeriodo(faltasPeriodo)
            .tipoPeriodo(tipo.name())
            .alumnosConPermisoAcademia(0L) // se calcula en Sprint 5
            .docentesAprobados(aprobados)
            .docentesPendientes(pendientes)
            .docentesRetrasados(retrasados)
            .alumnosMatriculadosCompletos(alumnosTotales - expedientesIncompletos)
            .alumnosMatriculaProvisional(expedientesIncompletos)
            .asistenciaPeriodo(asistenciaPeriodo)
            .build();
    }

    @Override
    public List<Map<String, Object>> obtenerAlertas(Integer anio) {
        int anioActual = anio != null ? anio : Year.now().getValue();
        long expedientesIncompletos = matriculaService.contarConExpedienteIncompleto(anioActual);

        return List.of(
            Map.of("tipo", "DOCUMENTOS", "titulo", "Documentos Faltantes",
                   "subtitulo", "DNI/Partidas sin entregar", "cantidad", expedientesIncompletos),
            Map.of("tipo", "MATRICULA", "titulo", "Matrículas Provisionales",
                   "subtitulo", "Pendientes de documentación completa", "cantidad", expedientesIncompletos)
        );
    }

    @Override
    public byte[] exportarResumenExcel(Integer anio, PeriodoResumen periodo, Integer mes) {
        DashboardKpiDTO kpis = obtenerKpis(anio, periodo, mes);
        int anioActual = anio != null ? anio : Year.now().getValue();
        return excelReportService.construirLibro("Resumen Dashboard",
            new String[]{"INDICADOR", "VALOR"}, construirFilasResumen(kpis, anioActual));
    }

    @Override
    public byte[] exportarResumenPdf(Integer anio, PeriodoResumen periodo, Integer mes) {
        DashboardKpiDTO kpis = obtenerKpis(anio, periodo, mes);
        int anioActual = anio != null ? anio : Year.now().getValue();
        return pdfReportService.construirDocumento("Resumen Dashboard - " + anioActual,
            new String[]{"INDICADOR", "VALOR"}, construirFilasResumen(kpis, anioActual));
    }

    private List<Object[]> construirFilasResumen(DashboardKpiDTO kpis, int anio) {
        return List.of(
            new Object[]{"Año académico", anio},
            new Object[]{"Período de asistencia", describirPeriodo(kpis.getTipoPeriodo())},
            new Object[]{"Alumnos totales", kpis.getAlumnosTotales()},
            new Object[]{"Docentes totales", kpis.getDocentesTotales()},
            new Object[]{"Aulas totales", kpis.getAulasTotales()},
            new Object[]{"Alumnos presentes en el período", kpis.getAlumnosPresentesPeriodo()},
            new Object[]{"Alumnos con falta en el período", kpis.getAlumnosFaltasPeriodo()},
            new Object[]{"Docentes aprobados (semáforo)", kpis.getDocentesAprobados()},
            new Object[]{"Docentes pendientes (semáforo)", kpis.getDocentesPendientes()},
            new Object[]{"Docentes retrasados (semáforo)", kpis.getDocentesRetrasados()},
            new Object[]{"Alumnos matriculados (expediente completo)", kpis.getAlumnosMatriculadosCompletos()},
            new Object[]{"Alumnos con matrícula provisional", kpis.getAlumnosMatriculaProvisional()}
        );
    }

    private String describirPeriodo(String tipoPeriodo) {
        return switch (tipoPeriodo) {
            case "MES" -> "Mensual";
            case "ANIO" -> "Anual";
            default -> "Semanal";
        };
    }
}
