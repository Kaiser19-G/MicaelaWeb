import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type PeriodoDashboard = 'SEMANA' | 'MES' | 'ANIO';

export interface AsistenciaDia {
  dia: string;
  alumnos: number;
  docentes: number;
}

export interface DashboardKpi {
  alumnosTotales: number;
  docentesTotales: number;
  aulasTotales: number;
  alumnosPresentesPeriodo: number;
  alumnosFaltasPeriodo: number;
  tipoPeriodo: PeriodoDashboard;
  alumnosConPermisoAcademia: number;
  docentesAprobados: number;
  docentesPendientes: number;
  docentesRetrasados: number;
  alumnosMatriculadosCompletos: number;
  alumnosMatriculaProvisional: number;
  asistenciaPeriodo: AsistenciaDia[];
}

export interface AlertaDashboard {
  tipo: string;
  titulo: string;
  subtitulo: string;
  cantidad: number;
}

/**
 * Servicio Angular para el Dashboard del Director.
 * Centraliza en una sola llamada todos los KPIs institucionales.
 * Base: /api/v1/dashboard
 */
@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  private readonly BASE = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  /**
   * Obtiene todos los KPIs del Director en una sola llamada. Las métricas de
   * asistencia (presentes/faltas/gráfico) se calculan sobre el período elegido.
   * Cumple con RNF-03: tiempo de respuesta < 2.5s.
   */
  obtenerKpis(anio?: number, periodo: PeriodoDashboard = 'SEMANA', mes?: number): Observable<DashboardKpi> {
    let params = new HttpParams().set('periodo', periodo);
    if (anio) params = params.set('anio', anio);
    if (mes) params = params.set('mes', mes);
    return this.http.get<DashboardKpi>(`${this.BASE}/kpis`, { params });
  }

  /** Obtiene las alertas activas del sistema. */
  obtenerAlertas(): Observable<AlertaDashboard[]> {
    return this.http.get<AlertaDashboard[]>(`${this.BASE}/alertas`);
  }

  /** Descarga en Excel el mismo resumen que ve el Director, filtrado por el período elegido. */
  exportar(anio?: number, periodo: PeriodoDashboard = 'SEMANA', mes?: number): Observable<Blob> {
    let params = new HttpParams().set('periodo', periodo);
    if (anio) params = params.set('anio', anio);
    if (mes) params = params.set('mes', mes);
    return this.http.get(`${this.BASE}/exportar`, { params, responseType: 'blob' });
  }

  /** Descarga en PDF el mismo resumen que ve el Director, filtrado por el período elegido. */
  exportarPdf(anio?: number, periodo: PeriodoDashboard = 'SEMANA', mes?: number): Observable<Blob> {
    let params = new HttpParams().set('periodo', periodo);
    if (anio) params = params.set('anio', anio);
    if (mes) params = params.set('mes', mes);
    return this.http.get(`${this.BASE}/exportar/pdf`, { params, responseType: 'blob' });
  }
}
