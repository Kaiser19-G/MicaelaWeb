import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Interceptor funcional JWT (Angular 19 standalone API).
 * Adjunta automáticamente el token Bearer a todas las peticiones HTTP.
 * Captura errores 401 para cerrar la sesión si el token expira.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  let modifiedReq = req;
  if (token) {
    modifiedReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(modifiedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Token expirado o credenciales inválidas
        alert('Tu sesión ha expirado por seguridad. Por favor, inicia sesión nuevamente.');
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};
