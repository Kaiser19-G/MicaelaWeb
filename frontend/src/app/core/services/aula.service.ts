import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AulaResponseDTO {
  id: number;
  grado: string;
  seccion: string;
  nivel: 'PRIMARIA' | 'SECUNDARIA';
  anioAcademico: number;
  capacidad: number;
  aulaReferencia?: string;
  descripcion: string;
  docentePrincipalId?: number;
  docentePrincipalNombre?: string;
  totalAlumnos: number;
  vacantesDisponibles: number;
}

export interface AulaRequestDTO {
  grado: string;
  seccion: string;
  nivel: 'PRIMARIA' | 'SECUNDARIA';
  anioAcademico: number;
  capacidad: number;
  aulaReferencia?: string;
  docentePrincipalId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AulaService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiUrl}/aulas`;

  listarPorAnio(anio: number): Observable<AulaResponseDTO[]> {
    return this.http.get<AulaResponseDTO[]>(this.BASE, { params: { anio } });
  }

  listarPorNivel(nivel: string, anio: number): Observable<AulaResponseDTO[]> {
    return this.http.get<AulaResponseDTO[]>(`${this.BASE}/nivel/${nivel}`, { params: { anio } });
  }

  buscarPorId(id: number): Observable<AulaResponseDTO> {
    return this.http.get<AulaResponseDTO>(`${this.BASE}/${id}`);
  }

  crear(dto: AulaRequestDTO): Observable<AulaResponseDTO> {
    return this.http.post<AulaResponseDTO>(this.BASE, dto);
  }

  /** Solo permitido si el aula no tiene alumnos activos ni cursos asignados. */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${id}`);
  }
}
