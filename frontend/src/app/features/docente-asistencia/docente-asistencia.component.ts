import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { AsistenciaService, RegistroAsistenciaDto, EstadoAsistencia, Asistencia } from '../../core/services/asistencia.service';
import { Subject, takeUntil } from 'rxjs';

interface AlumnoLista {
  id: number;
  nombreCompleto: string;
  codigoEstudiante: string;
  dni: string;
  tienePermisoAcademia: boolean;
  horaEntradaAcademia?: string;
  estado: EstadoAsistencia;
}

interface AulaSimple {
  id: number;
  descripcion: string;
}

@Component({
  selector: 'app-docente-asistencia',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './docente-asistencia.component.html',
  styleUrls: ['./docente-asistencia.component.scss']
})
export class DocenteAsistenciaComponent implements OnInit, OnDestroy {

  private asistenciaService = inject(AsistenciaService);
  private authService       = inject(AuthService);

  readonly cargando         = signal(false);
  readonly guardando        = signal(false);
  readonly modoOffline      = signal(false);
  readonly guardadoExitoso  = signal(false);

  fechaSeleccionada = this.hoy();
  aulaSeleccionada: number | null = null;
  docenteId = 1;

  aulas: AulaSimple[] = [];
  alumnos: AlumnoLista[] = [];

  readonly estadosRapidos: { valor: EstadoAsistencia; label: string; emoji: string }[] = [
    { valor: 'ASISTIO',   label: 'Asistió',  emoji: '✅' },
    { valor: 'FALTA',     label: 'Falta',    emoji: '❌' },
    { valor: 'TARDANZA',  label: 'Tardanza', emoji: '⏰' },
    { valor: 'LICENCIA',  label: 'Licencia', emoji: '📋' },
  ];

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.verificarConexion();
    this.cargarAulas();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private verificarConexion(): void {
    this.modoOffline.set(!navigator.onLine);
    window.addEventListener('online',  () => { this.modoOffline.set(false); this.sincronizarOffline(); });
    window.addEventListener('offline', () => this.modoOffline.set(true));
  }

  private cargarAulas(): void {
    this.aulas = [
      { id: 1, descripcion: '1ro A – Secundaria (2025)' },
      { id: 2, descripcion: '1ro B – Secundaria (2025)' },
      { id: 3, descripcion: '2do A – Secundaria (2025)' },
      { id: 4, descripcion: '3ro A – Secundaria (2025)' },
      { id: 5, descripcion: '4to A – Secundaria (2025)' },
      { id: 6, descripcion: '5to A – Secundaria (2025)' },
    ];
  }

  onAulaChange(): void {
    if (!this.aulaSeleccionada) return;
    this.cargarAlumnosDelAula();
  }

  private cargarAlumnosDelAula(): void {
    if (!this.aulaSeleccionada) return;
    this.cargando.set(true);

    this.asistenciaService.obtenerAsistenciaAula(this.aulaSeleccionada, this.fechaSeleccionada)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (asistencias: Asistencia[]) => {
          if (asistencias.length > 0) {
            this.alumnos = asistencias.map((a: Asistencia) => ({
              id: a.alumno.id,
              nombreCompleto: a.alumno.nombreCompleto,
              codigoEstudiante: '',
              dni: '',
              tienePermisoAcademia: a.aplicadoPermisoAcademia,
              horaEntradaAcademia: a.horaPermisoAcademia,
              estado: a.estado
            }));
          } else {
            this.cargarAlumnosBase();
          }
          this.cargando.set(false);
        },
        error: () => {
          this.cargarAlumnosBase();
          this.cargando.set(false);
        }
      });
  }

  private cargarAlumnosBase(): void {
    this.alumnos = [
      { id: 1, nombreCompleto: 'QUISPE FLORES, María Fernanda',  codigoEstudiante: 'IE-MB-2025-001', dni: '12345678', tienePermisoAcademia: false, estado: 'ASISTIO' },
      { id: 2, nombreCompleto: 'PAREDES HUANCA, José Luis',      codigoEstudiante: 'IE-MB-2025-002', dni: '23456789', tienePermisoAcademia: true,  horaEntradaAcademia: '13:30', estado: 'ASISTIO' },
      { id: 3, nombreCompleto: 'TORRES RAMOS, Ana Lucía',        codigoEstudiante: 'IE-MB-2025-003', dni: '34567890', tienePermisoAcademia: false, estado: 'ASISTIO' },
      { id: 4, nombreCompleto: 'MENDOZA CCALLO, Carlos Alberto', codigoEstudiante: 'IE-MB-2025-004', dni: '45678901', tienePermisoAcademia: true,  horaEntradaAcademia: '14:30', estado: 'ASISTIO' },
      { id: 5, nombreCompleto: 'GARCIA PAUCAR, Luciana Isabel',  codigoEstudiante: 'IE-MB-2025-005', dni: '56789012', tienePermisoAcademia: false, estado: 'ASISTIO' },
      { id: 6, nombreCompleto: 'VILCA MAMANI, Diego Alejandro',  codigoEstudiante: 'IE-MB-2025-006', dni: '67890123', tienePermisoAcademia: false, estado: 'ASISTIO' },
      { id: 7, nombreCompleto: 'CCOICA HUAYTA, Valeria Sophia',  codigoEstudiante: 'IE-MB-2025-007', dni: '78901234', tienePermisoAcademia: false, estado: 'ASISTIO' },
      { id: 8, nombreCompleto: 'LLERENA QUISPE, Rodrigo Mateo',  codigoEstudiante: 'IE-MB-2025-008', dni: '89012345', tienePermisoAcademia: false, estado: 'ASISTIO' },
    ];
  }

  marcarEstado(alumnoId: number, estado: EstadoAsistencia): void {
    const alumno = this.alumnos.find(a => a.id === alumnoId);
    if (!alumno) return;
    if (estado === 'TARDANZA' && alumno.tienePermisoAcademia) {
      alumno.estado = 'PERMISO_ACADEMIA';
    } else {
      alumno.estado = estado;
    }
  }

  marcarTodos(estado: EstadoAsistencia): void {
    this.alumnos.forEach(a => this.marcarEstado(a.id, estado));
  }

  guardarAsistencia(): void {
    if (!this.aulaSeleccionada) return;
    this.guardando.set(true);

    const registros: RegistroAsistenciaDto[] = this.alumnos.map(a => ({
      alumnoId: a.id,
      estado: a.estado,
    }));

    if (this.modoOffline()) {
      this.asistenciaService.guardarOfflineLocal(registros);
      this.guardando.set(false);
      this.guardadoExitoso.set(true);
      setTimeout(() => this.guardadoExitoso.set(false), 3000);
      return;
    }

    this.asistenciaService.registrarAsistenciaLote(
      this.aulaSeleccionada,
      this.fechaSeleccionada,
      this.docenteId,
      registros
    )
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: () => {
        this.guardando.set(false);
        this.guardadoExitoso.set(true);
        setTimeout(() => this.guardadoExitoso.set(false), 3000);
      },
      error: (err: unknown) => {
        console.error('Error guardando asistencia:', err);
        this.asistenciaService.guardarOfflineLocal(registros);
        this.guardando.set(false);
        this.modoOffline.set(true);
      }
    });
  }

  private sincronizarOffline(): void {
    if (!this.asistenciaService.hayRegistrosPendientesOffline()) return;
    console.log('Conexión restaurada – sincronizando registros offline...');
  }

  hoy(): string {
    return new Date().toISOString().split('T')[0];
  }

  get totalAsistieron(): number { return this.alumnos.filter(a => a.estado === 'ASISTIO' || a.estado === 'PERMISO_ACADEMIA').length; }
  get totalFaltas(): number     { return this.alumnos.filter(a => a.estado === 'FALTA').length; }
  get totalTardanzas(): number  { return this.alumnos.filter(a => a.estado === 'TARDANZA').length; }

  getClaseEstado(estado: EstadoAsistencia): string {
    const mapa: Record<string, string> = {
      'ASISTIO': 'estado-asistio', 'FALTA': 'estado-falta',
      'TARDANZA': 'estado-tardanza', 'LICENCIA': 'estado-licencia',
      'PERMISO_ACADEMIA': 'estado-academia', 'JUSTIFICADO': 'estado-justificado'
    };
    return mapa[estado] ?? '';
  }

  getEmojiEstado(estado: EstadoAsistencia): string {
    const mapa: Record<string, string> = {
      'ASISTIO': '✅', 'FALTA': '❌', 'TARDANZA': '⏰',
      'LICENCIA': '📋', 'PERMISO_ACADEMIA': '🎓', 'JUSTIFICADO': '📝'
    };
    return mapa[estado] ?? '';
  }
}
