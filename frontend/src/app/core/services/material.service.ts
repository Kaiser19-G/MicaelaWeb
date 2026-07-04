import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MaterialSemana {
  id?: number;
  semana: number;
  nombreArchivo: string;
  urlArchivo: string;
  fechaSubida?: string;
}

@Injectable({
  providedIn: 'root'
})
export class MaterialService {

  private readonly BASE = `${environment.apiUrl}/materiales`;

  constructor(private http: HttpClient) {}

  subirMaterial(cursoAsignadoId: number, semana: number, docenteId: number, archivo: File): Observable<MaterialSemana> {
    const formData = new FormData();
    formData.append('cursoAsignadoId', cursoAsignadoId.toString());
    formData.append('semana', semana.toString());
    formData.append('docenteId', docenteId.toString());
    formData.append('archivo', archivo);

    return this.http.post<MaterialSemana>(this.BASE, formData);
  }

  obtenerMateriales(cursoAsignadoId: number, semana: number): Observable<MaterialSemana[]> {
    return this.http.get<MaterialSemana[]>(`${this.BASE}/curso/${cursoAsignadoId}/semana/${semana}`);
  }

  listarPorCursoAsignado(cursoAsignadoId: number): Observable<MaterialSemana[]> {
    return this.http.get<MaterialSemana[]>(`${this.BASE}/curso/${cursoAsignadoId}`);
  }

  eliminarMaterial(materialId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${materialId}`);
  }
}
