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
  activo: boolean;
  aulaId?: number;
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
   * Obtiene los cursos asignados al aula del alumno
   */
  obtenerCursos(id: number): Observable<CursoAsignado[]> {
    return this.http.get<CursoAsignado[]>(`${this.BASE}/${id}/cursos`);
  }
}
