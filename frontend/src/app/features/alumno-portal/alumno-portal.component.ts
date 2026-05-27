import {
  Component,
  OnInit,
  OnDestroy,
  signal,
  computed,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil, forkJoin } from 'rxjs';

import { AsistenciaService, ResumenAsistencia, Asistencia } from '../../core/services/asistencia.service';
import { AuthService } from '../../core/services/auth.service';

// ── Interfaces locales ────────────────────────────────────────────────────

export type TabId = 'calificaciones' | 'asistencia' | 'horarios';

export interface Tab {
  id: TabId;
  label: string;
  icon: string;
}

export interface NotaAlumno {
  area: string;
  b1?: number;
  b2?: number;
  b3?: number;
  b4?: number;
  promedio?: number;
  literal?: 'AD' | 'A' | 'B' | 'C';
}

export interface HorarioCurso {
  hora: string;
  lunes?: string;
  martes?: string;
  miercoles?: string;
  jueves?: string;
  viernes?: string;
}

// ── Componente Principal ──────────────────────────────────────────────────

/**
 * AlumnoPortalComponent – Portal del Alumno con navegación por Tabs.
 *
 * Tabs:
 *  1. 📚 Calificaciones – Cuadro de notas por área curricular y bimestre
 *  2. 📅 Asistencia     – Calendario de inasistencias con estadísticas
 *  3. 🕐 Horarios       – Horario de clases semanal
 *
 * Arquitectura: Standalone Component (Angular 19 – sin NgModules).
 */
@Component({
  selector: 'app-alumno-portal',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './alumno-portal.component.html',
  styleUrls: ['./alumno-portal.component.scss']
})
export class AlumnoPortalComponent implements OnInit, OnDestroy {

  // ── Inyecciones ───────────────────────────────────────────────────────
  public asistenciaService = inject(AsistenciaService);
  public authService       = inject(AuthService);

  // ── Signals (Angular 19 Reactivity) ──────────────────────────────────
  readonly tabActiva   = signal<TabId>('calificaciones');
  readonly cargando    = signal(false);
  readonly alumnoId    = signal<number | null>(null);
  readonly anioActual  = signal(new Date().getFullYear());

  // ── Datos ─────────────────────────────────────────────────────────────
  readonly tabs: Tab[] = [
    { id: 'calificaciones', label: 'Calificaciones', icon: '📚' },
    { id: 'asistencia',     label: 'Asistencia',     icon: '📅' },
    { id: 'horarios',       label: 'Horarios',        icon: '🕐' }
  ];

  notas: NotaAlumno[] = [];
  resumenAsistencia: ResumenAsistencia | null = null;
  historialAsistencia: Asistencia[] = [];
  horario: HorarioCurso[] = this.generarHorarioEjemplo();

  // ── Computed Values ───────────────────────────────────────────────────
  readonly porcentajeAsistenciaColor = computed(() => {
    const pct = this.resumenAsistencia?.porcentajeAsistencia ?? 100;
    if (pct >= 85) return 'success';
    if (pct >= 70) return 'warning';
    return 'danger';
  });

  readonly totalFaltasInjustificadas = computed(() => {
    if (!this.resumenAsistencia) return 0;
    return this.resumenAsistencia.faltas;
  });

  /** Nombre del usuario para la template (evita acceso directo al servicio). */
  readonly usuarioNombre = computed(() => this.authService.getUsuarioActual()?.username ?? 'Alumno');
  readonly usuarioInicial = computed(() => (this.authService.getUsuarioActual()?.username ?? 'A')[0]?.toUpperCase() ?? 'A');

  // ── Ciclo de Vida ─────────────────────────────────────────────────────
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Obtener ID del alumno desde el perfil autenticado
    // En producción este dato viene del token JWT decodificado
    const idDesdeSession = 1; // TODO: obtener desde AuthService / token
    this.alumnoId.set(idDesdeSession);
    this.cargarDatosInicialesTab('calificaciones');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Navegación de Tabs ────────────────────────────────────────────────

  seleccionarTab(tab: TabId): void {
    if (this.tabActiva() === tab) return;
    this.tabActiva.set(tab);
    this.cargarDatosInicialesTab(tab);
  }

  esTabActiva(tab: TabId): boolean {
    return this.tabActiva() === tab;
  }

  // ── Carga de Datos por Tab ────────────────────────────────────────────

  private cargarDatosInicialesTab(tab: TabId): void {
    switch (tab) {
      case 'calificaciones':
        this.cargarNotas();
        break;
      case 'asistencia':
        this.cargarAsistencia();
        break;
      case 'horarios':
        // Los horarios se cargan en ngOnInit estáticamente por ahora
        break;
    }
  }

  private cargarNotas(): void {
    this.cargando.set(true);
    // TODO: Conectar con NotaService cuando esté implementado
    // Simulamos los datos para la vista inicial
    this.notas = [
      { area: 'Comunicación',      b1: 16, b2: 17, b3: 15, b4: 18, promedio: 16.5, literal: 'A' },
      { area: 'Matemática',        b1: 14, b2: 13, b3: 15, b4: 16, promedio: 14.5, literal: 'A' },
      { area: 'Ciencia y Tecnología', b1: 17, b2: 18, b3: 16, b4: 19, promedio: 17.5, literal: 'AD' },
      { area: 'Historia y Geografía', b1: 15, b2: 14, b3: 16, b4: 15, promedio: 15.0, literal: 'A' },
      { area: 'Inglés',            b1: 12, b2: 13, b3: 14, b4: 15, promedio: 13.5, literal: 'B' },
      { area: 'Educación Física',  b1: 18, b2: 19, b3: 17, b4: 18, promedio: 18.0, literal: 'AD' },
      { area: 'Arte y Cultura',    b1: 16, b2: 17, b3: 18, b4: 16, promedio: 16.75, literal: 'A' },
      { area: 'Tutoría',           b1: 18, b2: 18, b3: 18, b4: 18, promedio: 18.0, literal: 'AD' },
    ];
    this.cargando.set(false);
  }

  private cargarAsistencia(): void {
    const id = this.alumnoId();
    if (!id) return;

    this.cargando.set(true);
    const inicio = `${this.anioActual()}-03-01`;
    const fin    = `${this.anioActual()}-12-31`;

    forkJoin({
      resumen:  this.asistenciaService.obtenerResumenAlumno(id, inicio, fin),
      historial: this.asistenciaService.obtenerHistorialAlumno(id, inicio, fin)
    })
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: ({ resumen, historial }) => {
        this.resumenAsistencia    = resumen;
        this.historialAsistencia  = historial;
        this.cargando.set(false);
      },
      error: err => {
        console.error('Error cargando asistencia:', err);
        this.cargando.set(false);
        // Fallback con datos de ejemplo para la demo
        this.resumenAsistencia = {
          totalDias: 180, asistencias: 165, faltas: 8,
          tardanzas: 5, justificados: 2, permisosAcademia: 0,
          porcentajeAsistencia: 91.7
        };
      }
    });
  }

  // ── Helpers de Vista ──────────────────────────────────────────────────

  getLiteralClass(literal?: string): string {
    switch (literal) {
      case 'AD': return 'badge-ad';
      case 'A':  return 'badge-a';
      case 'B':  return 'badge-b';
      case 'C':  return 'badge-c';
      default:   return '';
    }
  }

  getEstadoAsistenciaClass(estado: string): string {
    const mapa: Record<string, string> = {
      'ASISTIO':          'estado-asistio',
      'FALTA':            'estado-falta',
      'TARDANZA':         'estado-tardanza',
      'JUSTIFICADO':      'estado-justificado',
      'PERMISO_ACADEMIA': 'estado-academia',
      'LICENCIA':         'estado-licencia'
    };
    return mapa[estado] ?? '';
  }

  formatearEstado(estado: string): string {
    const etiquetas: Record<string, string> = {
      'ASISTIO':          'Asistió',
      'FALTA':            'Falta',
      'TARDANZA':         'Tardanza',
      'JUSTIFICADO':      'Justificado',
      'PERMISO_ACADEMIA': 'Permiso Academia',
      'LICENCIA':         'Licencia'
    };
    return etiquetas[estado] ?? estado;
  }

  private generarHorarioEjemplo(): HorarioCurso[] {
    return [
      { hora: '07:30 – 08:15', lunes: 'Matemática', martes: 'Comunicación',   miercoles: 'CTA',       jueves: 'Historia',  viernes: 'Inglés'     },
      { hora: '08:15 – 09:00', lunes: 'Matemática', martes: 'Comunicación',   miercoles: 'CTA',       jueves: 'Historia',  viernes: 'Inglés'     },
      { hora: '09:00 – 09:45', lunes: 'Inglés',     martes: 'Matemática',     miercoles: 'Comunicación', jueves: 'Arte',   viernes: 'Ed. Física' },
      { hora: '09:45 – 10:00', lunes: '— Recreo —', martes: '— Recreo —',    miercoles: '— Recreo —', jueves: '— Recreo —', viernes: '— Recreo —' },
      { hora: '10:00 – 10:45', lunes: 'Historia',   martes: 'CTA',            miercoles: 'Matemática', jueves: 'Comunicación', viernes: 'Tutoría' },
      { hora: '10:45 – 11:30', lunes: 'Historia',   martes: 'CTA',            miercoles: 'Matemática', jueves: 'Comunicación', viernes: 'Tutoría' },
      { hora: '11:30 – 12:15', lunes: 'Arte',       martes: 'Ed. Física',     miercoles: 'Historia',  jueves: 'CTA',       viernes: 'Matemática' },
      { hora: '12:15 – 13:00', lunes: 'Arte',       martes: 'Ed. Física',     miercoles: 'Historia',  jueves: 'CTA',       viernes: 'Matemática' },
    ];
  }
}
