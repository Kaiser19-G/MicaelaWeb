import { Injectable, NgZone } from '@angular/core';
import localforage from 'localforage';
import { BehaviorSubject, Observable, from, fromEvent, merge, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { RegistroAsistenciaDto, Asistencia } from './asistencia.service';

export interface SyncQueueItem {
  id: string;
  aulaId: number;
  fecha: string;
  docenteId: number;
  registros: RegistroAsistenciaDto[];
  timestamp: number;
}

@Injectable({
  providedIn: 'root'
})
export class SyncService {
  private readonly ASISTENCIA_QUEUE_KEY = 'cola_asistencia_pendientes';
  private readonly CACHE_ALUMNOS_KEY_PREFIX = 'cache_alumnos_aula_';
  private readonly BASE_URL = `${environment.apiUrl}/asistencias`;

  // Estado de conexión reactivo
  private isOnlineSubject = new BehaviorSubject<boolean>(navigator.onLine);
  public isOnline$ = this.isOnlineSubject.asObservable();

  // Contador de elementos pendientes
  private pendingCountSubject = new BehaviorSubject<number>(0);
  public pendingCount$ = this.pendingCountSubject.asObservable();

  constructor(private http: HttpClient, private ngZone: NgZone) {
    this.initLocalForage();
    this.setupNetworkListeners();
    this.updatePendingCount();
  }

  private initLocalForage() {
    localforage.config({
      name: 'MicaelaBastidasApp',
      storeName: 'offline_store',
      description: 'Almacenamiento offline para asistencia y caché'
    });
  }

  private setupNetworkListeners() {
    // Usamos NgZone para asegurarnos de que Angular detecte los cambios
    window.addEventListener('online', () => {
      this.ngZone.run(() => {
        this.isOnlineSubject.next(true);
        this.syncPendingData();
      });
    });

    window.addEventListener('offline', () => {
      this.ngZone.run(() => {
        this.isOnlineSubject.next(false);
      });
    });
  }

  // ── Caché de Lectura (Alumnos) ──────────────────────────────────────────

  async saveAlumnosCache(aulaId: number, data: any[]): Promise<any> {
    const key = `${this.CACHE_ALUMNOS_KEY_PREFIX}${aulaId}`;
    return localforage.setItem(key, data);
  }

  async getAlumnosCache(aulaId: number): Promise<any[] | null> {
    const key = `${this.CACHE_ALUMNOS_KEY_PREFIX}${aulaId}`;
    return localforage.getItem<any[]>(key);
  }

  // ── Cola de Escritura (Asistencia) ──────────────────────────────────────

  async queueAsistencia(aulaId: number, fecha: string, docenteId: number, registros: RegistroAsistenciaDto[]): Promise<void> {
    const queue = await this.getQueue();
    const item: SyncQueueItem = {
      id: crypto.randomUUID(),
      aulaId,
      fecha,
      docenteId,
      registros,
      timestamp: Date.now()
    };
    
    queue.push(item);
    await localforage.setItem(this.ASISTENCIA_QUEUE_KEY, queue);
    this.updatePendingCount();
  }

  private async getQueue(): Promise<SyncQueueItem[]> {
    const queue = await localforage.getItem<SyncQueueItem[]>(this.ASISTENCIA_QUEUE_KEY);
    return queue || [];
  }

  private async updatePendingCount() {
    const queue = await this.getQueue();
    this.pendingCountSubject.next(queue.length);
  }

  // ── Sincronización Background ───────────────────────────────────────────

  public async syncPendingData(): Promise<void> {
    if (!navigator.onLine) return;

    const queue = await this.getQueue();
    if (queue.length === 0) return;

    // Procesamos cada elemento de la cola
    const newQueue: SyncQueueItem[] = [];
    
    for (const item of queue) {
      try {
        await this.sendToBackend(item);
      } catch (error) {
        console.error('Error sincronizando lote, se mantendrá en cola:', error);
        newQueue.push(item); // Si falla, lo mantenemos en la cola
      }
    }

    // Actualizamos la cola con los que fallaron (o vacía si todo fue bien)
    await localforage.setItem(this.ASISTENCIA_QUEUE_KEY, newQueue);
    this.updatePendingCount();
  }

  private sendToBackend(item: SyncQueueItem): Promise<void> {
    return new Promise((resolve, reject) => {
      const params = {
        aulaId: item.aulaId.toString(),
        fecha: item.fecha,
        docenteId: item.docenteId.toString()
      };

      this.http.post<any>(`${this.BASE_URL}/lote`, item.registros, { params }).subscribe({
        next: () => resolve(),
        error: (err) => reject(err)
      });
    });
  }

  // Utilidad para saber el estado actual directo
  public get isOnline(): boolean {
    return navigator.onLine;
  }
}
