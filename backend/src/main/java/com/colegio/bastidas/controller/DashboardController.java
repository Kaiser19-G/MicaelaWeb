package com.colegio.bastidas.controller;

import com.colegio.bastidas.dto.dashboard.DashboardKpiDTO;
import com.colegio.bastidas.dto.dashboard.PeriodoResumen;
import com.colegio.bastidas.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador del Dashboard del Director.
 * Base: {@code /api/v1/dashboard}
 *
 * Agrega todos los KPIs e indicadores estratégicos en un solo endpoint
 * para minimizar las llamadas HTTP desde el frontend (< 2.5 seg RNF-03).
 * Las métricas de asistencia (presentes/faltas/gráfico) respetan el período
 * elegido por el Director en el panel: SEMANA (actual), MES o ANIO.
 */
@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /dashboard/kpis?anio=2026&periodo=SEMANA|MES|ANIO&mes=3
     * Retorna todos los KPIs del Panel del Director en una sola llamada.
     */
    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<DashboardKpiDTO> obtenerKpis(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") Integer anio,
            @RequestParam(defaultValue = "SEMANA") String periodo,
            @RequestParam(required = false) Integer mes) {
        return ResponseEntity.ok(dashboardService.obtenerKpis(anio, PeriodoResumen.valueOf(periodo.toUpperCase()), mes));
    }

    /**
     * GET /dashboard/exportar?anio=2026&periodo=SEMANA|MES|ANIO&mes=3
     * Descarga en Excel el mismo resumen que ve el Director, filtrado por el período elegido.
     * Acceso: DIRECTOR, ADMIN
     */
    @GetMapping("/exportar")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") Integer anio,
            @RequestParam(defaultValue = "SEMANA") String periodo,
            @RequestParam(required = false) Integer mes) {
        PeriodoResumen tipo = PeriodoResumen.valueOf(periodo.toUpperCase());
        byte[] excel = dashboardService.exportarResumenExcel(anio, tipo, mes);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
            String.format("dashboard_resumen_%s.xlsx", nombreArchivoPeriodo(tipo, anio, mes)));
        return ResponseEntity.ok().headers(headers).body(excel);
    }

    /**
     * GET /dashboard/exportar/pdf?anio=2026&periodo=SEMANA|MES|ANIO&mes=3
     * Descarga en PDF el mismo resumen que ve el Director, filtrado por el período elegido.
     * Acceso: DIRECTOR, ADMIN
     */
    @GetMapping("/exportar/pdf")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") Integer anio,
            @RequestParam(defaultValue = "SEMANA") String periodo,
            @RequestParam(required = false) Integer mes) {
        PeriodoResumen tipo = PeriodoResumen.valueOf(periodo.toUpperCase());
        byte[] pdf = dashboardService.exportarResumenPdf(anio, tipo, mes);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
            String.format("dashboard_resumen_%s.pdf", nombreArchivoPeriodo(tipo, anio, mes)));
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    /**
     * GET /dashboard/alertas?anio=2026
     * Retorna las alertas activas (documentos faltantes, matrículas provisionales, etc.)
     */
    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerAlertas(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") Integer anio) {
        return ResponseEntity.ok(dashboardService.obtenerAlertas(anio));
    }

    private String nombreArchivoPeriodo(PeriodoResumen tipo, Integer anio, Integer mes) {
        return switch (tipo) {
            case MES -> String.format("mes_%02d_%d", mes != null ? mes : 1, anio);
            case ANIO -> String.format("anio_%d", anio);
            default -> "semana_actual";
        };
    }
}
