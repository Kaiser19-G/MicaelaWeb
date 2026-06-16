import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ── Tipos de escala ────────────────────────────────────────────────────────
export type EscalaLiteral   = 'AD' | 'A' | 'B' | 'C';
export type EscalaVigesimal = number; // 0 – 20
export type TipoEscala      = 'LITERAL' | 'VIGESIMAL';

// ── Interfaces ────────────────────────────────────────────────────────────

export interface NotaDto {
  alumnoId:      number;
  cursoId:       number;
  bimestre:      number;  // 1 | 2 | 3 | 4
  tipoEscala:    TipoEscala;
  notaLiteral?:  EscalaLiteral;   // solo si tipoEscala === 'LITERAL'
  notaVigesimal?: number;          // solo si tipoEscala === 'VIGESIMAL'
  observacion?:  string;
  evidenciaUrl?: string;           // URL devuelta por subirEvidencia()
}

export interface NotaResponse {
  id:            number;
  alumnoId:      number;
  cursoId:       number;
  bimestre:      number;
  tipoEscala:    TipoEscala;
  notaLiteral?:  EscalaLiteral;
  notaVigesimal?: number;
  observacion?:  string;
  evidenciaUrl?: string;
  fechaRegistro: string;
}

export interface EvidenciaResponse {
  url:          string;  // URL pública del archivo subido
  nombreArchivo: string;
  tamanioBytes:  number;
}

/** Fila editable de nota por alumno (uso interno del componente) */
export interface FilaNota {
  alumnoId:      number;
  nombreAlumno:  string;
  tipoEscala:    TipoEscala;
  notaLiteral?:  EscalaLiteral;
  notaVigesimal?: number | null;
  evidenciaUrl?: string;
  archivoPendiente?: File;   // archivo seleccionado, aún no subido
  conEvidencia:  boolean;    // flag rápido para validación
  modificado:    boolean;    // flag para marcar si hubo cambios
}

// ── Constantes de escala ──────────────────────────────────────────────────
export const ESCALA_LITERAL_OPCIONES: EscalaLiteral[] = ['AD', 'A', 'B', 'C'];

/**
 * NotaService – Servicio Angular para registrar notas y evidencias.
 *
 * Endpoints:
 *  - POST /api/v1/notas
 *  - POST /api/v1/evidencias/upload
 *
 * Escala:
 *  - LITERAL    → primaria   (AD / A / B / C)
 *  - VIGESIMAL  → secundaria (0 – 20)
 */
@Injectable({
  providedIn: 'root'
})
export class NotaService {

  private readonly BASE_NOTAS     = `${environment.apiUrl}/notas`;
  private readonly BASE_EVIDENCIAS = `${environment.apiUrl}/evidencias`;

  constructor(private http: HttpClient) {}

  // ── Notas ──────────────────────────────────────────────────────────────

  /**
   * Registra una nota individual para un alumno.
   * TODO: conectar cuando el backend esté disponible.
   */
  registrarNota(nota: NotaDto): Observable<NotaResponse> {
    return this.http.post<NotaResponse>(this.BASE_NOTAS, nota);
  }

  /**
   * Registra notas para un curso/bimestre completo (lote).
   * TODO: conectar cuando el backend esté disponible.
   */
  registrarNotasLote(notas: NotaDto[]): Observable<NotaResponse[]> {
    return this.http.post<NotaResponse[]>(`${this.BASE_NOTAS}/lote`, notas);
  }

  /**
   * Obtiene las notas de un curso para un bimestre específico.
   * TODO: conectar cuando el backend esté disponible.
   */
  obtenerNotasCurso(cursoId: number, bimestre: number): Observable<NotaResponse[]> {
    const params = new HttpParams().set('bimestre', bimestre);
    return this.http.get<NotaResponse[]>(`${this.BASE_NOTAS}/curso/${cursoId}`, { params });
  }

  // ── Evidencias ─────────────────────────────────────────────────────────

  /**
   * Sube un archivo de evidencia y retorna la URL pública.
   * POST /api/v1/evidencias/upload
   * TODO: conectar cuando el backend esté disponible.
   */
  subirEvidencia(archivo: File, alumnoId: number, cursoId: number): Observable<EvidenciaResponse> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    formData.append('alumnoId', String(alumnoId));
    formData.append('cursoId',  String(cursoId));
    return this.http.post<EvidenciaResponse>(`${this.BASE_EVIDENCIAS}/upload`, formData);
  }

  // ── Helpers ────────────────────────────────────────────────────────────

  /**
   * Determina el tipo de escala según el grado del curso.
   * Si el grado contiene 'Secundaria' → VIGESIMAL, si no → LITERAL.
   */
  inferirEscala(grado: string): TipoEscala {
    return grado.toLowerCase().includes('secundaria') ? 'VIGESIMAL' : 'LITERAL';
  }

  /**
   * Valida que una nota en escala vigesimal esté en rango [0, 20].
   */
  validarVigesimal(valor: number | null | undefined): boolean {
    if (valor === null || valor === undefined) return false;
    return valor >= 0 && valor <= 20;
  }

  /**
   * Construye filas de notas para la tabla del componente (con datos simulados).
   * TODO: reemplazar con llamada a obtenerNotasCurso() cuando el backend esté listo.
   */
  construirFilasSimuladas(
    alumnos: { id: number; nombre: string }[],
    grado: string
  ): FilaNota[] {
    const escala = this.inferirEscala(grado);
    return alumnos.map(a => ({
      alumnoId:      a.id,
      nombreAlumno:  a.nombre,
      tipoEscala:    escala,
      notaLiteral:   undefined,
      notaVigesimal: null,
      evidenciaUrl:  undefined,
      archivoPendiente: undefined,
      conEvidencia:  false,
      modificado:    false,
    }));
  }
}
