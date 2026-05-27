import { Routes } from '@angular/router';
import { AuthGuard, RoleGuard } from './core/guards/role.guard';

/**
 * Rutas principales de la aplicación.
 * Utiliza lazy loading para cada feature module (Standalone).
 */
export const routes: Routes = [

  // ── Ruta raíz ──────────────────────────────────────────────────────
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },

  // ── Autenticación ──────────────────────────────────────────────────
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
    title: 'Iniciar Sesión – I.E. Micaela Bastidas'
  },

  // ── Portal del Alumno ──────────────────────────────────────────────
  {
    path: 'alumno/portal',
    loadComponent: () =>
      import('./features/alumno-portal/alumno-portal.component').then(
        m => m.AlumnoPortalComponent
      ),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ALUMNO', 'DIRECTOR'] },
    title: 'Portal del Estudiante – I.E. Micaela Bastidas'
  },

  // ── Panel del Director ─────────────────────────────────────────────
  {
    path: 'admin-dashboard',
    loadComponent: () =>
      import('./features/admin-dashboard/admin-dashboard.component').then(
        m => m.AdminDashboardComponent
      ),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['DIRECTOR', 'ADMIN'] },
    title: 'Panel del Director – I.E. Micaela Bastidas'
  },

  // ── Vista Docente – Asistencia ──────────────────────────────────────
  {
    path: 'docente/asistencia',
    loadComponent: () =>
      import('./features/docente-asistencia/docente-asistencia.component').then(
        m => m.DocenteAsistenciaComponent
      ),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['DOCENTE', 'DIRECTOR'] },
    title: 'Control de Asistencia – I.E. Micaela Bastidas'
  },

  // ── Matrículas ──────────────────────────────────────────────────────
  {
    path: 'matriculas',
    loadComponent: () =>
      import('./features/matriculas/matriculas.component').then(
        m => m.MatriculasComponent
      ),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['DIRECTOR', 'ADMIN'] },
    title: 'Matrícula Digital – I.E. Micaela Bastidas'
  },

  // ── Fallback 404 ───────────────────────────────────────────────────
  {
    path: '**',
    redirectTo: 'login'
  }
];
