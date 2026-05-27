import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ── Modelos / Interfaces ───────────────────────────────────────────────────

export type EstadoAsistencia =
  | 'ASISTIO'
  | 'FALTA'
  | 'TARDANZA'
  | 'LICENCIA'
  | 'PERMISO_ACADEMIA'
  | 'JUSTIFICADO';

export interface RegistroAsistenciaDto {
  alumnoId: number;
  estado: EstadoAsistencia;
  justificacion?: string;
}

export interface ResumenAsistencia {
  totalDias: number;
  asistencias: number;
  faltas: number;
  tardanzas: number;
  justificados: number;
  permisosAcademia: number;
  porcentajeAsistencia: number;
}

export interface Asistencia {
  id: number;
  alumno: { id: number; nombreCompleto: string };
  aula: { id: number; descripcion: string };
  fecha: string;
  estado: EstadoAsistencia;
  horaLlegada?: string;
  minutosTardanza?: number;
  aplicadoPermisoAcademia: boolean;
  horaPermisoAcademia?: string;
  justificacion?: string;
  tieneJustificacion: boolean;
  sincronizadoOffline: boolean;
}

export interface AlertaFalta {
  alumnoId: number;
  nombreAlumno: string;
  totalFaltas: number;
}

export interface PermisoAcademia {
  tienePermiso: boolean;
  horaEntrada: string;
}

// ── Servicio ───────────────────────────────────────────────────────────────

/**
 * AsistenciaService – Servicio Angular para conectar con el backend Spring Boot.
 *
 * Implementa:
 *  - Registro individual y por lote (app móvil docente)
 *  - Sincronización de registros offline
 *  - Resumen e historial del alumno (Portal del Alumno – Pestaña 2)
 *  - Alertas de faltas excesivas (Panel Director)
 *  - Verificación de permiso de academia
 */
@Injectable({
  providedIn: 'root'
})
export class AsistenciaService {

  private readonly BASE_URL = `${environment.apiUrl}/asistencias`;

  constructor(private http: HttpClient) {}

  // ── Registro ──────────────────────────────────────────────────────────

  /**
   * Registra la asistencia de un alumno individual.
   */
  registrarAsistencia(
    alumnoId: number,
    fecha: string,          // formato: YYYY-MM-DD
    estado: EstadoAsistencia,
    docenteId: number
  ): Observable<Asistencia> {
    const params = new HttpParams()
      .set('alumnoId', alumnoId)
      .set('fecha', fecha)
      .set('estado', estado)
      .set('docenteId', docenteId);

    return this.http.post<Asistencia>(`${this.BASE_URL}/registro`, null, { params });
  }

  /**
   * Registro masivo de asistencia para un aula completa.
   * Optimizado para la vista móvil de marcado rápido del docente.
   */
  registrarAsistenciaLote(
    aulaId: number,
    fecha: string,
    docenteId: number,
    registros: RegistroAsistenciaDto[]
  ): Observable<Asistencia[]> {
    const params = new HttpParams()
      .set('aulaId', aulaId)
      .set('fecha', fecha)
      .set('docenteId', docenteId);

    return this.http.post<Asistencia[]>(`${this.BASE_URL}/lote`, registros, { params });
  }

  // ── Consultas ─────────────────────────────────────────────────────────

  /**
   * Obtiene las asistencias de un aula para la fecha dada (tiempo real).
   */
  obtenerAsistenciaAula(aulaId: number, fecha: string): Observable<Asistencia[]> {
    return this.http.get<Asistencia[]>(`${this.BASE_URL}/aula/${aulaId}`, {
      params: new HttpParams().set('fecha', fecha)
    });
  }

  /**
   * Resumen de asistencia del alumno para el Portal del Alumno (Pestaña 2 – Calendario).
   */
  obtenerResumenAlumno(
    alumnoId: number,
    inicio: string,
    fin: string
  ): Observable<ResumenAsistencia> {
    const params = new HttpParams().set('inicio', inicio).set('fin', fin);
    return this.http.get<ResumenAsistencia>(
      `${this.BASE_URL}/alumno/${alumnoId}/resumen`, { params }
    );
  }

  /**
   * Historial detallado de asistencias del alumno (para el calendario).
   */
  obtenerHistorialAlumno(
    alumnoId: number,
    inicio: string,
    fin: string
  ): Observable<Asistencia[]> {
    const params = new HttpParams().set('inicio', inicio).set('fin', fin);
    return this.http.get<Asistencia[]>(
      `${this.BASE_URL}/alumno/${alumnoId}/historial`, { params }
    );
  }

  // ── Offline Sync ──────────────────────────────────────────────────────

  /**
   * Sincroniza registros de asistencia capturados offline en el dispositivo del docente.
   * Los registros se guardan en localStorage y se sincronizan cuando hay conexión.
   */
  sincronizarRegistrosOffline(registros: Asistencia[]): Observable<{ sincronizados: number }> {
    return this.http.post<{ sincronizados: number }>(
      `${this.BASE_URL}/offline/sync`, registros
    );
  }

  // ── Permiso de Academia ───────────────────────────────────────────────

  /**
   * Verifica si el alumno tiene permiso de academia (entrada 13:30 / 14:30).
   */
  verificarPermisoAcademia(alumnoId: number): Observable<PermisoAcademia> {
    return this.http.get<PermisoAcademia>(
      `${this.BASE_URL}/alumno/${alumnoId}/permiso-academia`
    );
  }

  // ── Alertas (Panel Director) ──────────────────────────────────────────

  /**
   * Alumnos con faltas excesivas en el período (Monitor del Director).
   */
  obtenerAlertasFaltasExcesivas(
    aulaId: number,
    inicio: string,
    fin: string,
    minFaltas = 3
  ): Observable<AlertaFalta[]> {
    const params = new HttpParams()
      .set('aulaId', aulaId)
      .set('inicio', inicio)
      .set('fin', fin)
      .set('minFaltas', minFaltas);

    return this.http.get<AlertaFalta[]>(`${this.BASE_URL}/alertas/faltas-excesivas`, { params });
  }

  // ── Persistencia Offline Local ────────────────────────────────────────

  /**
   * Guarda registros offline en localStorage para sincronizar luego.
   */
  guardarOfflineLocal(asistencias: RegistroAsistenciaDto[]): void {
    const key = 'bastidas_asistencias_offline';
    const existentes = this.cargarOfflineLocal();
    const combinados = [...existentes, ...asistencias];
    localStorage.setItem(key, JSON.stringify(combinados));
  }

  cargarOfflineLocal(): RegistroAsistenciaDto[] {
    const raw = localStorage.getItem('bastidas_asistencias_offline');
    return raw ? JSON.parse(raw) : [];
  }

  limpiarOfflineLocal(): void {
    localStorage.removeItem('bastidas_asistencias_offline');
  }

  hayRegistrosPendientesOffline(): boolean {
    return this.cargarOfflineLocal().length > 0;
  }
}
