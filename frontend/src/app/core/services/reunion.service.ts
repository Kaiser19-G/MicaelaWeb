import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { generarLinkWhatsApp } from '../utils/whatsapp.util';

export interface ReunionRequestDTO {
  alumnoId?: number;
  fecha: string; // "YYYY-MM-DD"
  horaInicio: string; // "10:00"
  horaFin: string;    // "11:00"
  motivo: string;
}

export interface Reunion {
  id: number;
  alumnoId: number;
  nombreAlumno: string;
  nombreApoderado: string | null;
  celularApoderado: string | null;
  aulaDescripcion: string;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  motivo: string;
  estado: 'PENDIENTE' | 'CONFIRMADA' | 'REALIZADA' | 'CANCELADA';
  convocadaPorUsername: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReunionService {
  private http = inject(HttpClient);
  private readonly BASE = `${environment.apiUrl}/reuniones`;

  /** Agenda una reunión individual con el apoderado de un alumno. */
  crear(dto: ReunionRequestDTO): Observable<Reunion> {
    return this.http.post<Reunion>(this.BASE, dto);
  }

  /** Agenda una reunión general: una convocatoria por cada alumno del aula. */
  crearParaAula(aulaId: number, dto: ReunionRequestDTO): Observable<Reunion[]> {
    return this.http.post<Reunion[]>(`${this.BASE}/aula/${aulaId}`, dto);
  }

  listarProximas(): Observable<Reunion[]> {
    return this.http.get<Reunion[]>(`${this.BASE}/proximas`);
  }

  /** Construye el link wa.me para enviar el mensaje al apoderado manualmente. */
  static generarLinkWhatsApp(reunion: Reunion): string | null {
    const mensaje =
      `Estimado(a) ${reunion.nombreApoderado || 'apoderado'}, le escribimos de la I.E. Micaela Bastidas ` +
      `para invitarlo a una reunión sobre ${reunion.nombreAlumno} el ${reunion.fecha} ` +
      `de ${reunion.horaInicio} a ${reunion.horaFin}. Motivo: ${reunion.motivo}.`;
    return generarLinkWhatsApp(reunion.celularApoderado, mensaje);
  }
}
