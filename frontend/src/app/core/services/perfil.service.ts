import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Perfil {
  id: number;
  username: string;
  rol: string;
  nombreCompleto: string | null;
  celular: string | null;
  email: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class PerfilService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiUrl}/auth/perfil`;

  obtener(): Observable<Perfil> {
    return this.http.get<Perfil>(this.BASE);
  }

  actualizar(perfil: Pick<Perfil, 'nombreCompleto' | 'celular' | 'email'>): Observable<Perfil> {
    return this.http.put<Perfil>(this.BASE, perfil);
  }
}
