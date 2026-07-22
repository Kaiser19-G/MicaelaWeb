import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DocenteDocumentoResponse {
  id: number;
  nombreArchivo: string;
  urlPublica: string;
  createdAt: string;
}

/**
 * Documento opcional que certifica que el docente tiene o está cursando
 * un curso de docencia. Base: /api/v1/docentes/{id}/documentos
 */
@Injectable({
  providedIn: 'root'
})
export class DocenteDocumentoService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiUrl}/docentes`;

  subir(docenteId: number, archivo: File): Observable<{ urlDocumento: string }> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<{ urlDocumento: string }>(`${this.BASE}/${docenteId}/documentos`, formData);
  }

  /** Retorna null si el docente no tiene documento subido (204 No Content). */
  obtener(docenteId: number): Observable<DocenteDocumentoResponse | null> {
    return this.http.get<DocenteDocumentoResponse>(`${this.BASE}/${docenteId}/documentos`, { observe: 'response' }).pipe(
      map(res => res.body ?? null),
      catchError(() => of(null))
    );
  }

  eliminar(docenteId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${docenteId}/documentos`);
  }
}
