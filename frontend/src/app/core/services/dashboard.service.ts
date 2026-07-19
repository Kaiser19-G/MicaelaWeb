import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AsistenciaDia {
  dia: string;
  alumnos: number;
  docentes: number;
}

export interface DashboardKpi {
  alumnosTotales: number;
  docentesTotales: number;
  aulasTotales: number;
  alumnosPresentesHoy: number;
  alumnosFaltasHoy: number;
  alumnosConPermisoAcademia: number;
  docentesAprobados: number;
  docentesPendientes: number;
  docentesRetrasados: number;
  alumnosMatriculadosCompletos: number;
  alumnosMatriculaProvisional: number;
  asistenciaSemanal: AsistenciaDia[];
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
   * Obtiene todos los KPIs del Director en una sola llamada.
   * Cumple con RNF-03: tiempo de respuesta < 2.5s.
   */
  obtenerKpis(anio?: number): Observable<DashboardKpi> {
    let params = new HttpParams();
    if (anio) params = params.set('anio', anio);
    return this.http.get<DashboardKpi>(`${this.BASE}/kpis`, { params });
  }

  /** Obtiene las alertas activas del sistema. */
  obtenerAlertas(): Observable<AlertaDashboard[]> {
    return this.http.get<AlertaDashboard[]>(`${this.BASE}/alertas`);
  }

  /** Descarga un resumen de los KPIs del panel en Excel. */
  exportar(anio?: number): Observable<Blob> {
    let params = new HttpParams();
    if (anio) params = params.set('anio', anio);
    return this.http.get(`${this.BASE}/exportar`, { params, responseType: 'blob' });
  }
}
