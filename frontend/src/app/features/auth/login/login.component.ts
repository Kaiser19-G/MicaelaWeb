import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService, LoginRequest } from '../../../core/services/auth.service';

/** Componente de Login (esqueleto listo para estilizar). */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-wrapper">
      <div class="login-card">
        <div class="login-card__header">
          <span class="logo">🏫</span>
          <h1>I.E. Micaela Bastidas</h1>
          <p>Sistema de Gestión Escolar</p>
        </div>
        <form (ngSubmit)="onLogin()" #f="ngForm">
          <div class="form-group">
            <label for="username">Usuario</label>
            <input id="username" type="text" name="username"
              [(ngModel)]="credenciales.username" required
              placeholder="DNI o código de usuario" />
          </div>
          <div class="form-group">
            <label for="password">Contraseña</label>
            <input id="password" type="password" name="password"
              [(ngModel)]="credenciales.password" required
              placeholder="••••••••" />
          </div>
          @if (error) {
            <div class="error-msg">{{ error }}</div>
          }
          <button type="submit" [disabled]="cargando" class="btn-login">
            {{ cargando ? 'Ingresando...' : 'Ingresar' }}
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .login-wrapper {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #1a3a6b, #0f2549);
    }
    .login-card {
      background: white;
      border-radius: 16px;
      padding: 2.5rem;
      width: 100%;
      max-width: 400px;
      box-shadow: 0 20px 60px rgba(0,0,0,0.3);

    }
    .login-card__header {
      text-align: center;
      margin-bottom: 2rem;
      .logo { font-size: 3rem; display: block; }
      h1 { margin: 0.5rem 0 0.25rem; font-size: 1.3rem; color: #1a3a6b; }
      p { margin: 0; color: #64748b; font-size: 0.875rem; }
    }
    .form-group {
      margin-bottom: 1rem;
      label { display: block; margin-bottom: 0.4rem; font-weight: 500; color: #374151; font-size: 0.875rem; }
      input {
        width: 100%; padding: 0.7rem 1rem; border: 1.5px solid #e2e8f0;
        border-radius: 8px; font-size: 0.95rem; box-sizing: border-box;
        &:focus { outline: none; border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }
      }
    }
    .btn-login {
      width: 100%; padding: 0.85rem; background: #1a3a6b; color: white;
      border: none; border-radius: 8px; font-size: 1rem; font-weight: 600;
      cursor: pointer; margin-top: 0.5rem; transition: background 0.2s;
      &:hover:not(:disabled) { background: #2563eb; }
      &:disabled { opacity: 0.6; cursor: not-allowed; }
    }
    .error-msg {
      background: #fef2f2; color: #991b1b; padding: 0.6rem 0.9rem;
      border-radius: 8px; font-size: 0.85rem; margin-bottom: 0.75rem;
      border-left: 3px solid #ef4444;
    }
  `]
})
export class LoginComponent {
  credenciales: LoginRequest = { username: '', password: '' };
  cargando = false;
  error = '';

  constructor(private authService: AuthService, private router: Router) {}

  onLogin(): void {
    this.cargando = true;
    this.error = '';
    this.authService.login(this.credenciales).subscribe({
      next: (resp) => {
        // Redirigir según rol
        switch (resp.rol) {
          case 'DIRECTOR':
          case 'ADMIN':    this.router.navigate(['/admin-dashboard']); break;
          case 'DOCENTE':  this.router.navigate(['/docente/asistencia']); break;
          case 'ALUMNO':   this.router.navigate(['/alumno/portal']); break;
          default:         this.router.navigate(['/login']);
        }
        this.cargando = false;
      },
      error: () => {
        this.error = 'Credenciales incorrectas. Verifique su usuario y contraseña.';
        this.cargando = false;
      }
    });
  }
}
