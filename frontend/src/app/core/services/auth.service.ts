import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tipo: string;
  id?: number;
  username: string;
  rol: 'DIRECTOR' | 'DOCENTE' | 'ALUMNO' | 'ADMIN';
  primerLogin: boolean;
}

export interface UsuarioActual {
  id?: number;
  perfilId?: number;  // id del Docente o Alumno (no del Usuario)
  username: string;
  rol: AuthResponse['rol'];
  primerLogin: boolean;
}

/**
 * Servicio global de Autenticación JWT.
 * Almacena el token en localStorage y expone el usuario actual
 * como BehaviorSubject para reactividad en toda la app.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly TOKEN_KEY = 'bastidas_token';
  private readonly USER_KEY  = 'bastidas_user';
  private readonly BASE_URL  = `${environment.apiUrl}/auth`;

  private usuarioSubject = new BehaviorSubject<UsuarioActual | null>(
    this.cargarUsuarioLocal()
  );

  /** Observable del usuario autenticado actual. */
  readonly usuario$ = this.usuarioSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  // ── Login ──────────────────────────────────────────────────────────────

  login(credenciales: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.BASE_URL}/login`, credenciales).pipe(
      tap(response => {
        localStorage.setItem(this.TOKEN_KEY, response.token);
        const usuario: UsuarioActual = {
          id: response.id ?? undefined,
          perfilId: (response as any).perfilId ?? undefined,
          username:   response.username,
          rol:        response.rol,
          primerLogin: response.primerLogin
        };
        localStorage.setItem(this.USER_KEY, JSON.stringify(usuario));
        this.usuarioSubject.next(usuario);
      })
    );
  }

  // ── Cambio de Contraseña ───────────────────────────────────────────────

  changePassword(newPassword: string): Observable<any> {
    return this.http.put(`${this.BASE_URL}/change-password`, { newPassword }).pipe(
      tap(() => {
        // Actualizar el estado local
        const user = this.getUsuarioActual();
        if (user) {
          user.primerLogin = false;
          localStorage.setItem(this.USER_KEY, JSON.stringify(user));
          this.usuarioSubject.next(user);
        }
      })
    );
  }

  // ── Logout ─────────────────────────────────────────────────────────────

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.usuarioSubject.next(null);
    this.router.navigate(['/login']);
  }

  // ── Getters ────────────────────────────────────────────────────────────

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getUsuarioActual(): UsuarioActual | null {
    return this.usuarioSubject.getValue();
  }

  estaAutenticado(): boolean {
    return !!this.getToken();
  }

  tieneRol(rol: AuthResponse['rol']): boolean {
    return this.getUsuarioActual()?.rol === rol;
  }

  esDirector(): boolean  { return this.tieneRol('DIRECTOR'); }
  esDocente(): boolean   { return this.tieneRol('DOCENTE'); }
  esAlumno(): boolean    { return this.tieneRol('ALUMNO'); }

  // ── Privados ───────────────────────────────────────────────────────────

  private cargarUsuarioLocal(): UsuarioActual | null {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }
}
