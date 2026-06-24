import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SyncService } from './sync.service';
import localforage from 'localforage';
import { environment } from '../../../environments/environment';

describe('SyncService (Offline-First)', () => {
  let service: SyncService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SyncService]
    });
    service = TestBed.inject(SyncService);
    httpMock = TestBed.inject(HttpTestingController);

    // Limpiar localforage antes de cada prueba
    spyOn(localforage, 'setItem').and.returnValue(Promise.resolve(true));
    spyOn(localforage, 'getItem').and.returnValue(Promise.resolve([]));
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('debería agregar un registro a la cola cuando se llama a queueAsistencia', fakeAsync(() => {
    const registros = [{ alumnoId: 1, estado: 'PRESENTE', justificacion: null }];
    
    service.queueAsistencia(1, '2026-10-10', 10, registros);
    tick();

    expect(localforage.getItem).toHaveBeenCalledWith('cola_asistencia_pendientes');
    expect(localforage.setItem).toHaveBeenCalled();
  }));

  it('debería procesar la cola si hay internet', fakeAsync(() => {
    // Simulamos que hay elementos en la cola
    const mockQueue = [
      {
        id: '123',
        aulaId: 1,
        fecha: '2026-10-10',
        docenteId: 10,
        registros: [{ alumnoId: 1, estado: 'PRESENTE', justificacion: null }],
        timestamp: Date.now()
      }
    ];

    (localforage.getItem as jasmine.Spy).and.returnValue(Promise.resolve(mockQueue));

    // Forzamos estado online
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });

    service.syncPendingData();
    tick();

    const req = httpMock.expectOne(req => req.url.includes('/asistencias/lote'));
    expect(req.request.method).toBe('POST');
    req.flush({}); // Respondemos con éxito

    tick();
    
    // Debería vaciar la cola
    expect(localforage.setItem).toHaveBeenCalledWith('cola_asistencia_pendientes', []);
  }));
});
