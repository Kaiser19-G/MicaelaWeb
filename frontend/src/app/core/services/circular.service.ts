import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CircularRequestDTO {
  titulo: string;
  contenido: string;
  dirigidoA: 'TODOS' | 'DOCENTES' | 'ALUMNOS';
}

export interface Circular {
  id: number;
  titulo: string;
  contenido: string;
  dirigidoA: 'TODOS' | 'DOCENTES' | 'ALUMNOS';
  publicada: boolean;
  fechaPublicacion: string | null;
  publicadaPorUsername: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class CircularService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiUrl}/circulares`;

  listar(): Observable<Circular[]> {
    return this.http.get<Circular[]>(this.BASE);
  }

  crear(dto: CircularRequestDTO): Observable<Circular> {
    return this.http.post<Circular>(this.BASE, dto);
  }

  publicar(id: number): Observable<Circular> {
    return this.http.put<Circular>(`${this.BASE}/${id}/publicar`, {});
  }
}
