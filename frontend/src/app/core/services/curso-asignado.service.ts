import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CursoAsignado } from './docente.service';

export interface CursoAsignadoRequestDTO {
  docenteId: number;
  aulaId: number;
  areaCurricular: string;
  anioAcademico: number;
  diaSemana?: string;
  horaInicio?: string;
  horaFin?: string;
}

export interface HorarioRequestDTO {
  diaSemana: string;
  horaInicio: string;
  horaFin: string;
}

@Injectable({
  providedIn: 'root'
})
export class CursoAsignadoService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiUrl}/cursos-asignados`;

  crear(dto: CursoAsignadoRequestDTO): Observable<CursoAsignado> {
    return this.http.post<CursoAsignado>(this.BASE, dto);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${id}`);
  }

  /** Lista las asignaciones (con horario, si ya fue definido) de una aula. */
  listarPorAula(aulaId: number): Observable<CursoAsignado[]> {
    return this.http.get<CursoAsignado[]>(`${this.BASE}/aula/${aulaId}`);
  }

  /** Fija o actualiza el horario (día + hora) de una asignación ya creada. */
  actualizarHorario(id: number, dto: HorarioRequestDTO): Observable<CursoAsignado> {
    return this.http.patch<CursoAsignado>(`${this.BASE}/${id}/horario`, dto);
  }
}
