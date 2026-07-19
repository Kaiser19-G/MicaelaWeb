import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AulaValidacionDTO {
  aulaId: number;
  nombre: string;
  estado: 'ok' | 'advertencia' | 'error';
  estudiantes: number;
  notasCompletas: number;
  notasBlanco: number;
  inconsistencias: number;
}

export interface CalidadResumen {
  totalAulas: number;
  aulasOk: number;
  conAdvertencias: number;
  conErrores: number;
  estudiantesConNotas: number;
  estudiantesTotal: number;
}

@Injectable({
  providedIn: 'root'
})
export class CalidadService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiUrl}/calidad`;

  listarValidacionAulas(anio: number): Observable<AulaValidacionDTO[]> {
    return this.http.get<AulaValidacionDTO[]>(`${this.BASE}/aulas`, { params: { anio: String(anio) } });
  }

  obtenerResumen(anio: number): Observable<CalidadResumen> {
    return this.http.get<CalidadResumen>(`${this.BASE}/resumen`, { params: { anio: String(anio) } });
  }
}
