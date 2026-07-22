import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DocenteResponse {
  id: number;
  codigoDocente: string;
  dni: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  nombres: string;
  nombreCompleto: string;
  especialidad: string;
  condicion: 'NOMBRADO' | 'CONTRATADO';
  emailInstitucional: string;
  celular: string;
  estadoCurricular: 'APROBADO' | 'PENDIENTE' | 'RETRASADO';
  cantidadEvidencias: number;
  cantidadCursosAsignados: number;
  activo: boolean;
  tieneCertificacionDocencia: boolean;
}

export interface SemaforoConteo {
  aprobados: number;
  pendientes: number;
  retrasados: number;
}

export interface DocenteRequestDTO {
  dni: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  nombres: string;
  especialidad?: string;
  condicion?: 'NOMBRADO' | 'CONTRATADO';
  emailInstitucional?: string;
  celular?: string;
}

export interface DocenteCreadoResponse {
  docente: DocenteResponse;
  usernameGenerado: string;
  passwordInicial: string;
}

export interface CursoAsignado {
  id: number;
  areaCurricular: string;
  anioAcademico: number;
  aula: {
    id: number;
    grado: string;
    seccion: string;
    nivel: string;
  };
  docente?: {
    nombres: string;
    apellidoPaterno: string;
    apellidoMaterno: string;
    nombreCompleto: string;
  };
  diaSemana?: 'LUNES' | 'MARTES' | 'MIERCOLES' | 'JUEVES' | 'VIERNES';
  horaInicio?: string; // "HH:mm:ss"
  horaFin?: string;
}

/**
 * Servicio Angular para consumir la API de Docentes.
 * Base: /api/v1/docentes
 */
@Injectable({
  providedIn: 'root'
})
export class DocenteService {

  private readonly BASE = `${environment.apiUrl}/docentes`;

  constructor(private http: HttpClient) {}

  /** Lista todos los docentes. */
  listarTodos(): Observable<DocenteResponse[]> {
    return this.http.get<DocenteResponse[]>(this.BASE);
  }

  /** Obtiene la lista completa con estado del Semáforo Curricular. */
  obtenerSemaforo(): Observable<DocenteResponse[]> {
    return this.http.get<DocenteResponse[]>(`${this.BASE}/semaforo`);
  }

  /** KPIs de conteo del Semáforo para el Dashboard. */
  obtenerConteosemaforo(): Observable<SemaforoConteo> {
    return this.http.get<SemaforoConteo>(`${this.BASE}/semaforo/conteo`);
  }

  /** Busca un docente por ID. */
  buscarPorId(id: number): Observable<DocenteResponse> {
    return this.http.get<DocenteResponse>(`${this.BASE}/${id}`);
  }

  /** Busca un docente por su DNI. */
  buscarPorDni(dni: string): Observable<DocenteResponse> {
    return this.http.get<DocenteResponse>(`${this.BASE}/dni/${dni}`);
  }

  /** Obtiene los cursos asignados a un docente */
  obtenerCursos(id: number): Observable<CursoAsignado[]> {
    return this.http.get<CursoAsignado[]>(`${this.BASE}/${id}/cursos`);
  }

  /** Registra un nuevo docente; el backend crea su cuenta de usuario automáticamente. */
  crear(dto: DocenteRequestDTO): Observable<DocenteCreadoResponse> {
    return this.http.post<DocenteCreadoResponse>(this.BASE, dto);
  }

  /** Actualiza los datos de un docente existente (no cambia DNI). */
  actualizar(id: number, dto: DocenteRequestDTO): Observable<DocenteResponse> {
    return this.http.put<DocenteResponse>(`${this.BASE}/${id}`, dto);
  }

  /** Da de baja (o reactiva) al docente: desactiva su cuenta, ya no puede iniciar sesión. */
  actualizarEstado(id: number, activo: boolean): Observable<DocenteResponse> {
    return this.http.patch<DocenteResponse>(`${this.BASE}/${id}/estado`, {}, { params: { activo: String(activo) } });
  }

  /** Descarga la lista de docentes con sus cursos asignados en Excel. */
  exportar(): Observable<Blob> {
    return this.http.get(`${this.BASE}/exportar`, { responseType: 'blob' });
  }

  /** Descarga la lista de docentes con sus cursos asignados en PDF. */
  exportarPdf(): Observable<Blob> {
    return this.http.get(`${this.BASE}/exportar/pdf`, { responseType: 'blob' });
  }
}
