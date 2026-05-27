import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard funcional de autenticación (Angular 19).
 * Bloquea el acceso si el usuario no tiene sesión activa.
 */
export const AuthGuard: CanActivateFn = (_route, state) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (auth.estaAutenticado()) return true;

  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};

/**
 * Guard funcional por rol (Angular 19).
 * Redirige al portal propio del usuario si intenta acceder a una ruta
 * que no le corresponde por rol.
 */
export const RoleGuard: CanActivateFn = (route) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  const usuario = auth.getUsuarioActual();
  if (!usuario) {
    router.navigate(['/login']);
    return false;
  }

  const rolesPermitidos: string[] = route.data?.['roles'] ?? [];

  if (rolesPermitidos.length === 0 || rolesPermitidos.includes(usuario.rol)) {
    return true;
  }

  // Usuario autenticado pero sin el rol requerido → redirige a su portal
  const portalPorRol: Record<string, string> = {
    DIRECTOR: '/admin-dashboard',
    DOCENTE:  '/docente/asistencia',
    ALUMNO:   '/alumno/portal',
  };
  router.navigate([portalPorRol[usuario.rol] ?? '/login']);
  return false;
};
