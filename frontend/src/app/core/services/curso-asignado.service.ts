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
}
