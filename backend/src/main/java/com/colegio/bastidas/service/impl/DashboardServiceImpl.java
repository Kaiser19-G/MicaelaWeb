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
    public DashboardKpiDTO obtenerKpis(Integer anio) {
        LocalDate hoy = LocalDate.now();
        int anioActual = anio != null ? anio : Year.now().getValue();

        long alumnosTotales  = alumnoRepository.contarAlumnosActivosPorAnio(anioActual);
        long docentesTotales = docenteRepository.count();
        long aulasTotales    = aulaService.contarPorAnio(anioActual);

        long presentesHoy = asistenciaRepository.countByFechaAndEstado(hoy, Asistencia.EstadoAsistencia.ASISTIO);
        long faltasHoy = asistenciaRepository.countByFechaAndEstado(hoy, Asistencia.EstadoAsistencia.FALTA);

        long aprobados  = docenteService.contarPorEstadoCurricular("APROBADO");
        long pendientes = docenteService.contarPorEstadoCurricular("PENDIENTE");
        long retrasados = docenteService.contarPorEstadoCurricular("RETRASADO");

        List<DashboardKpiDTO.AsistenciaDiaDTO> semanal =
            obtenerAsistenciaPorPeriodo(PeriodoResumen.SEMANA, anioActual, null);

        long expedientesIncompletos = matriculaService.contarConExpedienteIncompleto(anioActual);

        return DashboardKpiDTO.builder()
            .alumnosTotales(alumnosTotales)
            .docentesTotales(docentesTotales)
            .aulasTotales(aulasTotales)
            .alumnosPresentesHoy(presentesHoy)
            .alumnosFaltasHoy(faltasHoy)
            .alumnosConPermisoAcademia(0L) // se calcula en Sprint 5
            .docentesAprobados(aprobados)
            .docentesPendientes(pendientes)
            .docentesRetrasados(retrasados)
            .alumnosMatriculadosCompletos(alumnosTotales - expedientesIncompletos)
            .alumnosMatriculaProvisional(expedientesIncompletos)
            .asistenciaSemanal(semanal)
            .build();
    }

    @Override
    public List<DashboardKpiDTO.AsistenciaDiaDTO> obtenerAsistenciaPorPeriodo(PeriodoResumen periodo, Integer anio, Integer mes) {
        LocalDate hoy = LocalDate.now();
        PeriodoResumen tipo = periodo != null ? periodo : PeriodoResumen.SEMANA;
        List<PeriodoBucketHelper.Bucket> buckets = PeriodoBucketHelper.construirBuckets(tipo, hoy, anio, mes);

        LocalDate inicio = buckets.get(0).inicio();
        LocalDate fin = buckets.get(buckets.size() - 1).fin();
        List<Object[]> filas = asistenciaRepository.countAsistioPorFechaEntre(null, inicio, fin);

        return PeriodoBucketHelper.aplicarConteos(buckets, filas);
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
    public byte[] exportarResumenExcel(Integer anio) {
        int anioActual = anio != null ? anio : Year.now().getValue();
        return excelReportService.construirLibro("Resumen Dashboard",
            new String[]{"INDICADOR", "VALOR"}, construirFilasResumen(anioActual));
    }

    @Override
    public byte[] exportarResumenPdf(Integer anio) {
        int anioActual = anio != null ? anio : Year.now().getValue();
        return pdfReportService.construirDocumento("Resumen Dashboard - " + anioActual,
            new String[]{"INDICADOR", "VALOR"}, construirFilasResumen(anioActual));
    }

    private List<Object[]> construirFilasResumen(int anio) {
        DashboardKpiDTO kpis = obtenerKpis(anio);
        return List.of(
            new Object[]{"Año académico", anio},
            new Object[]{"Alumnos totales", kpis.getAlumnosTotales()},
            new Object[]{"Docentes totales", kpis.getDocentesTotales()},
            new Object[]{"Aulas totales", kpis.getAulasTotales()},
            new Object[]{"Alumnos presentes hoy", kpis.getAlumnosPresentesHoy()},
            new Object[]{"Alumnos con falta hoy", kpis.getAlumnosFaltasHoy()},
            new Object[]{"Docentes aprobados (semáforo)", kpis.getDocentesAprobados()},
            new Object[]{"Docentes pendientes (semáforo)", kpis.getDocentesPendientes()},
            new Object[]{"Docentes retrasados (semáforo)", kpis.getDocentesRetrasados()},
            new Object[]{"Alumnos matriculados (expediente completo)", kpis.getAlumnosMatriculadosCompletos()},
            new Object[]{"Alumnos con matrícula provisional", kpis.getAlumnosMatriculaProvisional()}
        );
    }
}
