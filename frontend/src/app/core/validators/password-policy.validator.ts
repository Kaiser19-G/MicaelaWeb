import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const MAYUSCULA = /[A-ZÁÉÍÓÚÑ]/;
const MINUSCULA = /[a-záéíóúñ]/;
const ESPECIAL = /[^A-Za-z0-9ÁÉÍÓÚÑáéíóúñ]/;
const LONGITUD_MINIMA = 8;

export const MENSAJE_PASSWORD_INVALIDA =
  'La contraseña debe tener al menos 8 caracteres, una letra mayúscula, una minúscula y un carácter especial.';

export function esPasswordValida(password: string): boolean {
  if (!password || password.length < LONGITUD_MINIMA) return false;
  return MAYUSCULA.test(password) && MINUSCULA.test(password) && ESPECIAL.test(password);
}

export function passwordComplexityValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string;
    if (!value) return null;
    return esPasswordValida(value) ? null : { passwordComplexity: true };
  };
}
