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

// ── Tipos ─────────────────────────────────────────────────────────────────
export type TabCurso   = 'silabo' | 'contenido' | 'evaluaciones' | 'tareas' | 'notas' | 'asistencia';
export type SidebarNav = 'cursos' | 'asistencia' | 'notas' | 'horario' | 'mensajes' | 'configuracion';

// ── Interfaces ────────────────────────────────────────────────────────────
export interface Materia {
  nombre: string;
  codigo: string;
  grado: string;
  modalidad: 'Presencial' | 'Virtual';
}

export type EstadoActividad = 'entregada' | 'vencida' | 'por_entregar' | 'no_revisado';

export interface ActividadBimestre {
  tipo: 'tarea' | 'evaluacion' | 'material';
  esCalificada: boolean;
  subtipo: string;
  nombre: string;
  estado: EstadoActividad;
  desde: string;
  hasta: string;
}

export interface SemanaData {
  numero: number;
  actividades: ActividadBimestre[];
}

export interface BimestreData {
  numero: number;
  nombre: string;
  periodo: string;
  semanas: SemanaData[];
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

// ── Componente ────────────────────────────────────────────────────────────

/**
 * AlumnoPortalComponent – Portal del Alumno estilo UTP+class.
 * Sidebar oscuro + header + navegación por materia y tabs.
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

  // ── Signals ───────────────────────────────────────────────────────────
  readonly sidebarNav       = signal<SidebarNav>('cursos');
  readonly tabCurso         = signal<TabCurso>('contenido');
  readonly materiaActiva    = signal<string>('Matemática');
  readonly cargando         = signal(false);
  readonly alumnoId         = signal<number | null>(null);
  readonly anioActual       = signal(new Date().getFullYear());
  readonly menuExpanded     = signal(false);

  // Bimestres expandidos (array de números)
  private bimestresExpandidos = signal<number[]>([1]);

  // ── Computed ──────────────────────────────────────────────────────────
  readonly usuarioNombre = computed(() =>
    this.authService.getUsuarioActual()?.username ?? 'Alumno'
  );
  readonly usuarioInicial = computed(() =>
    (this.authService.getUsuarioActual()?.username ?? 'A')[0]?.toUpperCase() ?? 'A'
  );

  readonly porcentajeAsistenciaColor = computed(() => {
    const pct = this.resumenAsistencia?.porcentajeAsistencia ?? 100;
    if (pct >= 85) return 'success';
    if (pct >= 70) return 'warning';
    return 'danger';
  });

  // ── Datos ─────────────────────────────────────────────────────────────
  readonly materias: Materia[] = [
    { nombre: 'Matemática',           codigo: 'MAT', grado: '5to A', modalidad: 'Presencial' },
    { nombre: 'Comunicación',         codigo: 'COM', grado: '5to A', modalidad: 'Presencial' },
    { nombre: 'CTA',                  codigo: 'CTA', grado: '5to A', modalidad: 'Presencial' },
    { nombre: 'Historia y Geografía', codigo: 'HGE', grado: '5to A', modalidad: 'Presencial' },
    { nombre: 'Inglés',               codigo: 'ING', grado: '5to A', modalidad: 'Presencial' },
    { nombre: 'Ed. Física',           codigo: 'EDF', grado: '5to A', modalidad: 'Presencial' },
    { nombre: 'Arte y Cultura',       codigo: 'ART', grado: '5to A', modalidad: 'Presencial' },
    { nombre: 'Tutoría',              codigo: 'TUT', grado: '5to A', modalidad: 'Presencial' },
  ];

  readonly bimestres: BimestreData[] = [
    {
      numero: 1, nombre: 'Bimestre 1', periodo: 'Mar – May',
      semanas: [
        {
          numero: 1, actividades: [
            { tipo: 'evaluacion', esCalificada: false, subtipo: 'No calificada', nombre: 'Examen de Entrada', estado: 'vencida', desde: '25/03/26 04:20 p.m.', hasta: '25/03/26 06:15 p.m.' },
            { tipo: 'material',   esCalificada: false, subtipo: 'No calificada', nombre: 'Material PDF Semana 01', estado: 'no_revisado', desde: '25/03/26 08:00 a.m.', hasta: '25/03/26 11:59 p.m.' },
          ]
        },
        {
          numero: 2, actividades: [
            { tipo: 'tarea', esCalificada: true, subtipo: 'Calificada – Avance de proyecto final 1', nombre: 'Actividad Nº 01', estado: 'entregada', desde: '01/04/26 05:55 p.m.', hasta: '01/04/26 06:15 p.m.' },
            { tipo: 'material', esCalificada: false, subtipo: 'No calificada', nombre: 'Material PDF Semana 02', estado: 'no_revisado', desde: '01/04/26 08:00 a.m.', hasta: '07/04/26 11:59 p.m.' },
          ]
        },
        {
          numero: 3, actividades: [
            { tipo: 'tarea', esCalificada: true, subtipo: 'Calificada – Avance de proyecto final 1', nombre: 'Actividad Nº 02', estado: 'vencida', desde: '15/04/26 05:26 p.m.', hasta: '15/04/26 06:00 p.m.' },
          ]
        },
        {
          numero: 4, actividades: [
            { tipo: 'material', esCalificada: false, subtipo: 'No calificada', nombre: 'Material Semana 04', estado: 'no_revisado', desde: '22/04/26 08:00 a.m.', hasta: '28/04/26 11:59 p.m.' },
            { tipo: 'evaluacion', esCalificada: false, subtipo: 'No calificada', nombre: 'AppS04', estado: 'no_revisado', desde: '24/04/26 08:00 a.m.', hasta: '28/04/26 11:59 p.m.' },
          ]
        },
      ]
    },
    {
      numero: 2, nombre: 'Bimestre 2', periodo: 'Jun – Jul',
      semanas: [
        {
          numero: 5, actividades: [
            { tipo: 'material', esCalificada: false, subtipo: 'No calificada', nombre: 'Material SEMANA 05', estado: 'no_revisado', desde: '02/06/26 08:00 a.m.', hasta: '08/06/26 11:59 p.m.' },
            { tipo: 'evaluacion', esCalificada: true, subtipo: 'Calificada – Avance de proyecto', nombre: 'PRIMER AVANCE DE PROYECTO', estado: 'vencida', desde: '05/06/26 09:00 a.m.', hasta: '05/06/26 11:59 p.m.' },
          ]
        },
        {
          numero: 6, actividades: [
            { tipo: 'material', esCalificada: false, subtipo: 'No calificada', nombre: 'Material SEMANA 06', estado: 'no_revisado', desde: '09/06/26 08:00 a.m.', hasta: '15/06/26 11:59 p.m.' },
            { tipo: 'tarea', esCalificada: false, subtipo: 'No calificada', nombre: 'AppS06', estado: 'no_revisado', desde: '13/06/26 08:00 a.m.', hasta: '15/06/26 11:59 p.m.' },
          ]
        },
      ]
    },
    {
      numero: 3, nombre: 'Bimestre 3', periodo: 'Ago – Sep',
      semanas: [
        { numero: 9,  actividades: [] },
        { numero: 10, actividades: [] },
      ]
    },
    {
      numero: 4, nombre: 'Bimestre 4', periodo: 'Oct – Nov',
      semanas: [
        { numero: 13, actividades: [] },
        { numero: 14, actividades: [] },
      ]
    },
  ];

  notas: NotaAlumno[] = [];
  resumenAsistencia: ResumenAsistencia | null = null;
  historialAsistencia: Asistencia[] = [];
  horario: HorarioCurso[] = this.generarHorario();

  readonly sidebarItems = [
    { id: 'cursos'        as SidebarNav, label: 'Cursos',         icon: 'book' },
    { id: 'notas'         as SidebarNav, label: 'Notas',          icon: 'chart' },
    { id: 'asistencia'    as SidebarNav, label: 'Asistencia',     icon: 'calendar' },
    { id: 'horario'       as SidebarNav, label: 'Horario',        icon: 'clock' },
    { id: 'mensajes'      as SidebarNav, label: 'Mensajes',       icon: 'mail' },
    { id: 'configuracion' as SidebarNav, label: 'Configuración',  icon: 'settings' },
  ];

  readonly cursoTabs: { id: TabCurso; label: string }[] = [
    { id: 'silabo',       label: 'Sílabo' },
    { id: 'contenido',    label: 'Contenido' },
    { id: 'evaluaciones', label: 'Evaluaciones' },
    { id: 'tareas',       label: 'Tareas' },
    { id: 'notas',        label: 'Notas' },
    { id: 'asistencia',   label: 'Asistencia' },
  ];

  // ── Ciclo de vida ─────────────────────────────────────────────────────
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.alumnoId.set(1);
    this.cargarNotas();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Navegación ─────────────────────────────────────────────────────────
  setSidebarNav(nav: SidebarNav): void { this.sidebarNav.set(nav); }
  setTabCurso(tab: TabCurso): void {
    this.tabCurso.set(tab);
    if (tab === 'asistencia') this.cargarAsistencia();
  }
  setMateria(nombre: string): void { this.materiaActiva.set(nombre); }

  getMateriaActiva(): Materia {
    return this.materias.find(m => m.nombre === this.materiaActiva()) ?? this.materias[0];
  }

  // ── Acordeón bimestres ──────────────────────────────────────────────────
  toggleBimestre(num: number): void {
    const curr = this.bimestresExpandidos();
    if (curr.includes(num)) {
      this.bimestresExpandidos.set(curr.filter(n => n !== num));
    } else {
      this.bimestresExpandidos.set([...curr, num]);
    }
  }

  esBimestreExpandido(num: number): boolean {
    return this.bimestresExpandidos().includes(num);
  }

  // ── Labels de estado ───────────────────────────────────────────────────
  getEstadoLabel(estado: EstadoActividad): string {
    const mapa: Record<EstadoActividad, string> = {
      entregada:    'Entregada',
      vencida:      'Vencida',
      por_entregar: 'Por entregar',
      no_revisado:  'No revisado',
    };
    return mapa[estado];
  }

  // ── Helpers de asistencia ──────────────────────────────────────────────
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

  getLiteralClass(literal?: string): string {
    switch (literal) {
      case 'AD': return 'badge-ad';
      case 'A':  return 'badge-a';
      case 'B':  return 'badge-b';
      case 'C':  return 'badge-c';
      default:   return '';
    }
  }

  // ── Carga de datos ─────────────────────────────────────────────────────
  private cargarNotas(): void {
    this.notas = [
      { area: 'Comunicación',         b1: 16, b2: 17, b3: 15, b4: 18, promedio: 16.5, literal: 'A'  },
      { area: 'Matemática',           b1: 14, b2: 13, b3: 15, b4: 16, promedio: 14.5, literal: 'A'  },
      { area: 'Ciencia y Tecnología', b1: 17, b2: 18, b3: 16, b4: 19, promedio: 17.5, literal: 'AD' },
      { area: 'Historia y Geografía', b1: 15, b2: 14, b3: 16, b4: 15, promedio: 15.0, literal: 'A'  },
      { area: 'Inglés',               b1: 12, b2: 13, b3: 14, b4: 15, promedio: 13.5, literal: 'B'  },
      { area: 'Educación Física',     b1: 18, b2: 19, b3: 17, b4: 18, promedio: 18.0, literal: 'AD' },
      { area: 'Arte y Cultura',       b1: 16, b2: 17, b3: 18, b4: 16, promedio: 16.75, literal: 'A' },
      { area: 'Tutoría',              b1: 18, b2: 18, b3: 18, b4: 18, promedio: 18.0, literal: 'AD' },
    ];
  }

  cargarAsistencia(): void {
    const id = this.alumnoId();
    if (!id) return;

    this.cargando.set(true);
    const inicio = `${this.anioActual()}-03-01`;
    const fin    = `${this.anioActual()}-12-31`;

    forkJoin({
      resumen:   this.asistenciaService.obtenerResumenAlumno(id, inicio, fin),
      historial: this.asistenciaService.obtenerHistorialAlumno(id, inicio, fin)
    })
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: ({ resumen, historial }) => {
        this.resumenAsistencia   = resumen;
        this.historialAsistencia = historial;
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.resumenAsistencia = {
          totalDias: 180, asistencias: 165, faltas: 8,
          tardanzas: 5, justificados: 2, permisosAcademia: 0,
          porcentajeAsistencia: 91.7
        };
      }
    });
  }

  private generarHorario(): HorarioCurso[] {
    return [
      { hora: '07:30 – 08:15', lunes: 'Matemática',    martes: 'Comunicación',    miercoles: 'CTA',       jueves: 'Historia',    viernes: 'Inglés'     },
      { hora: '08:15 – 09:00', lunes: 'Matemática',    martes: 'Comunicación',    miercoles: 'CTA',       jueves: 'Historia',    viernes: 'Inglés'     },
      { hora: '09:00 – 09:45', lunes: 'Inglés',        martes: 'Matemática',      miercoles: 'Comunicación', jueves: 'Arte',     viernes: 'Ed. Física' },
      { hora: '09:45 – 10:00', lunes: '— Recreo —',   martes: '— Recreo —',     miercoles: '— Recreo —', jueves: '— Recreo —', viernes: '— Recreo —' },
      { hora: '10:00 – 10:45', lunes: 'Historia',      martes: 'CTA',             miercoles: 'Matemática', jueves: 'Comunicación', viernes: 'Tutoría' },
      { hora: '10:45 – 11:30', lunes: 'Historia',      martes: 'CTA',             miercoles: 'Matemática', jueves: 'Comunicación', viernes: 'Tutoría' },
      { hora: '11:30 – 12:15', lunes: 'Arte',          martes: 'Ed. Física',      miercoles: 'Historia',  jueves: 'CTA',          viernes: 'Matemática' },
      { hora: '12:15 – 13:00', lunes: 'Arte',          martes: 'Ed. Física',      miercoles: 'Historia',  jueves: 'CTA',          viernes: 'Matemática' },
    ];
  }

  // Retorna las actividades de evaluaciones o tareas para la tab activa
  getActividadesFiltradas(tipo: 'evaluacion' | 'tarea'): { bimestre: number; semana: number; act: ActividadBimestre }[] {
    const result: { bimestre: number; semana: number; act: ActividadBimestre }[] = [];
    for (const b of this.bimestres) {
      for (const s of b.semanas) {
        for (const a of s.actividades) {
          if (a.tipo === tipo) result.push({ bimestre: b.numero, semana: s.numero, act: a });
        }
      }
    }
    return result;
  }
}
