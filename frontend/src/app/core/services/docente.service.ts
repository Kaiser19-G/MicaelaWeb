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
}

export interface SemaforoConteo {
  aprobados: number;
  pendientes: number;
  retrasados: number;
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
}
