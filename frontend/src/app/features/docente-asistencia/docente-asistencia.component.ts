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
  horaEntradaAcademia?: string;  // formato HH:mm
  estado: EstadoAsistencia;
}

/** Estructura de los niveles/grados/secciones disponibles */
interface NivelOpt  { id: string; nombre: string; }
interface GradoOpt  { id: string; nombre: string; nivelId: string; }
interface SeccionOpt { id: string; nombre: string; gradoId: string; aulaId: number; }

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

  // ── Selectores en cascada: Nivel → Grado → Sección ─────────────────────────
  // TODO: reemplazar con llamada a GET /api/v1/aulas/estructura
  nivelSeleccionado:   string | null = null;
  gradoSeleccionado:   string | null = null;
  seccionSeleccionada: string | null = null;

  readonly niveles: NivelOpt[] = [
    { id: 'primaria',   nombre: 'Primaria' },
    { id: 'secundaria', nombre: 'Secundaria' },
  ];

  readonly todosGrados: GradoOpt[] = [
    { id: '1p', nombre: '1er Grado',  nivelId: 'primaria' },
    { id: '2p', nombre: '2do Grado',  nivelId: 'primaria' },
    { id: '3p', nombre: '3er Grado',  nivelId: 'primaria' },
    { id: '4p', nombre: '4to Grado',  nivelId: 'primaria' },
    { id: '5p', nombre: '5to Grado',  nivelId: 'primaria' },
    { id: '6p', nombre: '6to Grado',  nivelId: 'primaria' },
    { id: '1s', nombre: '1ro',        nivelId: 'secundaria' },
    { id: '2s', nombre: '2do',        nivelId: 'secundaria' },
    { id: '3s', nombre: '3ro',        nivelId: 'secundaria' },
    { id: '4s', nombre: '4to',        nivelId: 'secundaria' },
    { id: '5s', nombre: '5to',        nivelId: 'secundaria' },
  ];

  readonly todasSecciones: SeccionOpt[] = [
    { id: 's1p1a', nombre: 'Sección A', gradoId: '1p', aulaId: 10 },
    { id: 's1p1b', nombre: 'Sección B', gradoId: '1p', aulaId: 11 },
    { id: 's2p1a', nombre: 'Sección A', gradoId: '2p', aulaId: 12 },
    { id: 's3p1a', nombre: 'Sección A', gradoId: '3p', aulaId: 13 },
    { id: 's4p1a', nombre: 'Sección A', gradoId: '4p', aulaId: 14 },
    { id: 's5p1a', nombre: 'Sección A', gradoId: '5p', aulaId: 15 },
    { id: 's6p1a', nombre: 'Sección A', gradoId: '6p', aulaId: 16 },
    { id: 's1s1a', nombre: 'Sección A', gradoId: '1s', aulaId: 1 },
    { id: 's1s1b', nombre: 'Sección B', gradoId: '1s', aulaId: 2 },
    { id: 's2s1a', nombre: 'Sección A', gradoId: '2s', aulaId: 3 },
    { id: 's3s1a', nombre: 'Sección A', gradoId: '3s', aulaId: 4 },
    { id: 's4s1a', nombre: 'Sección A', gradoId: '4s', aulaId: 5 },
    { id: 's5s1a', nombre: 'Sección A', gradoId: '5s', aulaId: 6 },
  ];

  /** Grados filtrados según el nivel seleccionado */
  get gradosFiltrados(): GradoOpt[] {
    if (!this.nivelSeleccionado) return [];
    return this.todosGrados.filter(g => g.nivelId === this.nivelSeleccionado);
  }

  /** Secciones filtradas según el grado seleccionado */
  get seccionesFiltradas(): SeccionOpt[] {
    if (!this.gradoSeleccionado) return [];
    return this.todasSecciones.filter(s => s.gradoId === this.gradoSeleccionado);
  }

  aulas: NivelOpt[] = []; // mantenido por compatibilidad (no se usa con cascada)
  alumnos: AlumnoLista[] = [];

  readonly estadosRapidos: { valor: EstadoAsistencia; label: string; emoji: string }[] = [
    { valor: 'ASISTIO',     label: 'Presente',    emoji: '✅' },
    { valor: 'FALTA',       label: 'Falta',        emoji: '❌' },
    { valor: 'TARDANZA',    label: 'Tardanza',     emoji: '⏰' },
    { valor: 'JUSTIFICADO', label: 'Justificado',  emoji: '📝' },
    { valor: 'LICENCIA',    label: 'Licencia',     emoji: '📋' },
  ];

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.verificarConexion();
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


  // ── Cascada: cambios en selectores ───────────────────────────────────────────
  onNivelChange(): void {
    this.gradoSeleccionado   = null;
    this.seccionSeleccionada = null;
    this.aulaSeleccionada    = null;
    this.alumnos = [];
  }

  onGradoChange(): void {
    this.seccionSeleccionada = null;
    this.aulaSeleccionada    = null;
    this.alumnos = [];
  }

  onSeccionChange(): void {
    // Buscar el aulaId correspondiente a la sección seleccionada
    const seccion = this.todasSecciones.find(s => s.id === this.seccionSeleccionada);
    this.aulaSeleccionada = seccion ? seccion.aulaId : null;
    if (this.aulaSeleccionada) {
      this.cargarAlumnosDelAula();
    }
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
    // Si el alumno tiene permiso academia y se marca TARDANZA, cambia a PERMISO_ACADEMIA
    if (estado === 'TARDANZA' && alumno.tienePermisoAcademia) {
      alumno.estado = 'PERMISO_ACADEMIA';
    } else {
      alumno.estado = estado;
    }
  }

  /**
   * Determina si el botón FALTA debe estar deshabilitado para un alumno.
   * Regla: si tiene permiso de academia y su hora de entrada aún no llegó,
   * no corresponde marcarlo como Falta.
   */
  debeDeshabilitarFalta(alumno: AlumnoLista): boolean {
    if (!alumno.tienePermisoAcademia || !alumno.horaEntradaAcademia) return false;
    const ahora = new Date();
    const [h, m] = alumno.horaEntradaAcademia.split(':').map(Number);
    const horaAcademia = new Date();
    horaAcademia.setHours(h, m, 0, 0);
    return ahora < horaAcademia; // si no llegó su hora, deshabilitar FALTA
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
