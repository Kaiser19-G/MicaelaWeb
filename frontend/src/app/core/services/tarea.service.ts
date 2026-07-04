import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Tarea {
  id?: number;
  titulo: string;
  descripcion?: string;
  fechaCreacion?: string;
  fechaLimite?: string;
  tipoCalificacion?: string;
}

@Injectable({
  providedIn: 'root'
})
export class TareaService {
  private readonly BASE = `${environment.apiUrl}/tareas`;

  constructor(private http: HttpClient) {}

  crearTarea(
    cursoAsignadoId: number, 
    semana: number, 
    titulo: string, 
    descripcion: string, 
    fechaLimite: string, 
    docenteId: number
  ): Observable<Tarea> {
    const params = new HttpParams()
      .set('cursoAsignadoId', cursoAsignadoId.toString())
      .set('semana', semana.toString())
      .set('titulo', titulo)
      .set('descripcion', descripcion)
      .set('fechaLimite', fechaLimite)
      .set('docenteId', docenteId.toString());

    return this.http.post<Tarea>(this.BASE, null, { params });
  }

  obtenerTareasPorCursoYSemana(cursoAsignadoId: number, semana: number): Observable<Tarea[]> {
    return this.http.get<Tarea[]>(`${this.BASE}/curso/${cursoAsignadoId}/semana/${semana}`);
  }
}
