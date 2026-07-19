import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MatriculaDto {
  id?: number;
  alumnoId: number;
  nombreAlumno?: string;
  codigoAlumno?: string;
  aulaId: number;
  grado?: string;   // Solo lectura (derivado del aula)
  seccion?: string; // Solo lectura (derivado del aula)
  anioEscolar: number;
  estado?: 'ACTIVO' | 'RETIRADO' | 'TRASLADADO';
}

@Injectable({
  providedIn: 'root'
})
export class MatriculaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/matriculas-crud`;

  listarPorAnio(anio: number): Observable<MatriculaDto[]> {
    return this.http.get<MatriculaDto[]>(`${this.apiUrl}/anio/${anio}`);
  }

  crear(dto: MatriculaDto): Observable<MatriculaDto> {
    return this.http.post<MatriculaDto>(this.apiUrl, dto);
  }

  actualizar(id: number, dto: MatriculaDto): Observable<MatriculaDto> {
    return this.http.put<MatriculaDto>(`${this.apiUrl}/${id}`, dto);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /** Descarga el consolidado de matrícula en Excel (formato SIAGIE). */
  exportarSiagie(anio: number, aulaId?: number): Observable<Blob> {
    const params: Record<string, string> = { anio: String(anio) };
    if (aulaId) params['aulaId'] = String(aulaId);
    return this.http.get(`${environment.apiUrl}/matriculas/exportar/siagie`, {
      params,
      responseType: 'blob'
    });
  }
}
