/**
 * Catálogo fijo de áreas curriculares (Currículo Nacional de Educación Básica, Perú).
 * `especialista: true` = requiere un docente dedicado en PRIMARIA (Arte, Ed. Física, etc.).
 * `especialista: false` = área que el docente-tutor de aula cubre en PRIMARIA.
 */
export interface AreaCurricular {
  nombre: string;
  especialista: boolean;
}

export const AREAS_CURRICULARES: AreaCurricular[] = [
  { nombre: 'Matemática', especialista: false },
  { nombre: 'Comunicación', especialista: false },
  { nombre: 'Ciencia y Tecnología', especialista: false },
  { nombre: 'Ciencias Sociales', especialista: false },
  { nombre: 'Desarrollo Personal, Ciudadanía y Cívica', especialista: false },
  { nombre: 'Educación para el Trabajo', especialista: false },
  { nombre: 'Arte y Cultura', especialista: true },
  { nombre: 'Educación Física', especialista: true },
  { nombre: 'Inglés', especialista: true },
  { nombre: 'Educación Religiosa', especialista: true },
];

/** Áreas que un docente-tutor de aula de PRIMARIA cubre por defecto. */
export const AREAS_TUTOR_PRIMARIA = AREAS_CURRICULARES
  .filter(a => !a.especialista)
  .map(a => a.nombre);
