import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CursoAsignado } from './docente.service';

export interface AlumnoResponse {
  id: number;
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  nombreCompleto: string;
  codigoEstudiante: string;
  dni: string;
  fechaNacimiento: string;
  tienePermisoAcademia: boolean;
  horaEntradaAcademia?: string;
  activo: boolean;
  aulaId?: number;
  aulaDescripcion?: string;
  anioAcademico?: number;
  estadoMatricula?: 'ACTIVO' | 'RETIRADO' | 'TRASLADADO' | 'EGRESADO';
  nombreApoderado?: string;
  celularApoderado?: string;
  emailApoderado?: string;
  tutorId?: number;
  tutorNombre?: string;
}

export interface AlumnoRequestDTO {
  dni: string;
  apellidoPaterno: string;
  apellidoMaterno: string;
  nombres: string;
  fechaNacimiento?: string; // "YYYY-MM-DD"
  sexo?: 'MASCULINO' | 'FEMENINO';
  anioAcademico: number;
  aulaId?: number;
  tienePermisoAcademia?: boolean;
  horaEntradaAcademia?: string;
  nombreApoderado?: string;
  celularApoderado?: string;
  emailApoderado?: string;
  tutorId?: number;
}

export interface AlumnoCreadoResponse {
  alumno: AlumnoResponse;
  usernameGenerado: string;
  passwordInicial: string;
}

@Injectable({
  providedIn: 'root'
})
export class AlumnoService {
  private readonly BASE = `${environment.apiUrl}/alumnos`;

  constructor(private http: HttpClient) {}

  /**
   * Obtiene la lista de alumnos matriculados en una sección específica
   */
  listarPorAula(aulaId: number): Observable<AlumnoResponse[]> {
    return this.http.get<AlumnoResponse[]>(`${this.BASE}/aula/${aulaId}`);
  }

  /**
   * Busca un alumno por su DNI.
   */
  buscarPorDni(dni: string): Observable<AlumnoResponse> {
    return this.http.get<AlumnoResponse>(`${this.BASE}/dni/${dni}`);
  }

  /**
   * Busca un alumno por su ID.
   */
  buscarPorId(id: number): Observable<AlumnoResponse> {
    return this.http.get<AlumnoResponse>(`${this.BASE}/${id}`);
  }

  /**
   * Obtiene los cursos asignados al aula del alumno
   */
  obtenerCursos(id: number): Observable<CursoAsignado[]> {
    return this.http.get<CursoAsignado[]>(`${this.BASE}/${id}/cursos`);
  }

  /** Registra un nuevo alumno; el backend crea su cuenta de usuario automáticamente. */
  crear(dto: AlumnoRequestDTO): Observable<AlumnoCreadoResponse> {
    return this.http.post<AlumnoCreadoResponse>(this.BASE, dto);
  }

  /** Actualiza los datos de un alumno existente (no cambia DNI ni aula). */
  actualizar(id: number, dto: AlumnoRequestDTO): Observable<AlumnoResponse> {
    return this.http.put<AlumnoResponse>(`${this.BASE}/${id}`, dto);
  }

  /** Cambia el estado de matrícula (baja lógica): ACTIVO, RETIRADO, TRASLADADO, EGRESADO. */
  actualizarEstado(id: number, estado: string): Observable<AlumnoResponse> {
    return this.http.patch<AlumnoResponse>(`${this.BASE}/${id}/estado`, {}, { params: { estado } });
  }

  /** Lista los alumnos con permiso de academia pro-universitaria (Control de Ingresos Especiales). */
  listarConPermisoAcademia(anio: number): Observable<AlumnoResponse[]> {
    return this.http.get<AlumnoResponse[]>(`${this.BASE}/academia`, { params: { anio: String(anio) } });
  }
}
