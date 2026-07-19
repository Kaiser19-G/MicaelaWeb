import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type TipoDocumentoExpediente =
  | 'DNI'
  | 'PARTIDA_NACIMIENTO'
  | 'CERTIFICADO_ESTUDIOS'
  | 'LIBRETA_NOTAS_ANTERIOR'
  | 'FOTO_CARNET'
  | 'CONSTANCIA_SALUD'
  | 'FICHA_MATRICULA'
  | 'OTRO';

export interface ExpedienteDocumento {
  id: number;
  tipoDocumento: TipoDocumentoExpediente;
  nombreArchivo: string;
  urlPublica: string;
  estadoVerificacion: string;
  createdAt: string;
}

export interface ExpedienteResumen {
  alumnoId: number;
  nombreCompleto: string;
  dni: string;
  aulaDescripcion: string | null;
  tieneDni: boolean;
  tienePartidaNacimiento: boolean;
  totalDocumentos: number;
  completo: boolean;
}

/** Documentos requeridos para la matrícula (ver Lineamientos del Proyecto). */
export const DOCUMENTOS_REQUERIDOS: { tipo: TipoDocumentoExpediente; etiqueta: string; obligatorio: boolean }[] = [
  { tipo: 'DNI', etiqueta: 'DNI del Estudiante', obligatorio: true },
  { tipo: 'PARTIDA_NACIMIENTO', etiqueta: 'Partida de Nacimiento', obligatorio: true },
  { tipo: 'CERTIFICADO_ESTUDIOS', etiqueta: 'Certificado de Estudios', obligatorio: false },
  { tipo: 'LIBRETA_NOTAS_ANTERIOR', etiqueta: 'Libreta de Notas (año anterior)', obligatorio: false },
  { tipo: 'FOTO_CARNET', etiqueta: 'Foto Carnet', obligatorio: false },
  { tipo: 'CONSTANCIA_SALUD', etiqueta: 'Constancia de Salud', obligatorio: false },
  { tipo: 'FICHA_MATRICULA', etiqueta: 'Ficha de Matrícula', obligatorio: false },
];

@Injectable({
  providedIn: 'root'
})
export class ExpedienteService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiUrl}/matriculas`;

  listarResumen(anio: number): Observable<ExpedienteResumen[]> {
    return this.http.get<ExpedienteResumen[]>(`${this.BASE}/expedientes/resumen`, {
      params: { anio: String(anio) }
    });
  }

  listarDocumentos(alumnoId: number): Observable<ExpedienteDocumento[]> {
    return this.http.get<ExpedienteDocumento[]>(`${this.BASE}/${alumnoId}/documentos`);
  }

  subirDocumento(alumnoId: number, archivo: File, tipoDocumento: TipoDocumentoExpediente): Observable<{ urlDocumento: string; tipoDocumento: string }> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    formData.append('tipoDocumento', tipoDocumento);
    return this.http.post<{ urlDocumento: string; tipoDocumento: string }>(
      `${this.BASE}/${alumnoId}/documentos`, formData);
  }

  eliminarDocumento(documentoId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/documentos/${documentoId}`);
  }
}
