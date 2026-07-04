import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CalificacionDiaria {
  id?: number;
  alumnoId: number;
  calificacion: string; // A, B, C, AD, NP
  fechaRegistro?: string;
}

export interface CalificacionDiariaRequest {
  alumnoId: number;
  calificacion: string;
}

@Injectable({
  providedIn: 'root'
})
export class CalificacionService {

  private readonly BASE = `${environment.apiUrl}/calificaciones-diarias`;

  constructor(private http: HttpClient) {}

  guardarLote(cursoAsignadoId: number, semana: number, docenteId: number, calificaciones: CalificacionDiariaRequest[]): Observable<CalificacionDiaria[]> {
    return this.http.post<CalificacionDiaria[]>(`${this.BASE}/batch`, calificaciones, {
      params: {
        cursoAsignadoId: cursoAsignadoId.toString(),
        semana: semana.toString(),
        docenteId: docenteId.toString()
      }
    });
  }

  obtenerPorCursoYSemana(cursoAsignadoId: number, semana: number): Observable<CalificacionDiaria[]> {
    return this.http.get<CalificacionDiaria[]>(`${this.BASE}/curso/${cursoAsignadoId}/semana/${semana}`);
  }
}
