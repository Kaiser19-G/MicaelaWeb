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
import { AlumnoService } from '../../core/services/alumno.service';
import { MaterialService } from '../../core/services/material.service';

// ── Tipos ─────────────────────────────────────────────────────────────────
export type TabCurso   = 'silabo' | 'contenido' | 'evaluaciones' | 'tareas' | 'notas' | 'asistencia';
export type SidebarNav = 'cursos' | 'asistencia' | 'notas' | 'horario' | 'mensajes' | 'configuracion';
export type CursosVista = 'lista' | 'detalle';

// ── Interfaces ────────────────────────────────────────────────────────────
export interface CursoCard {
  id: number;
  nombre: string;
  codigo: string;
  grado: string;
  modalidad: 'Presencial' | 'Virtual';
  docente: string;
  docente2?: string;
  progreso: number;        // 0-100
  color: string;           // color de fondo de la card
  icono: string;           // emoji o SVG key
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
  url?: string;
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
 * Pantalla inicio: grid de cards de cursos.
 * Pantalla detalle: tabs por curso (Sílabo, Contenido, Evaluaciones, etc.)
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
  public alumnoService     = inject(AlumnoService);
  public materialService   = inject(MaterialService);

  // ── Signals ───────────────────────────────────────────────────────────
  readonly sidebarNav       = signal<SidebarNav>('cursos');
  readonly cursosVista      = signal<CursosVista>('lista');   // 'lista' = cards, 'detalle' = tabs
  readonly tabCurso         = signal<TabCurso>('contenido');
  readonly cursoActivo      = signal<CursoCard | null>(null);
  readonly cargando         = signal(false);
  readonly alumnoId         = signal<number | null>(null);
  readonly anioActual       = signal(new Date().getFullYear());
  readonly filtroPeriodo    = signal('actual');

  // Bimestres expandidos
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

  // ── Cursos del alumno (datos del colegio) ─────────────────────────────
  readonly cursos = signal<CursoCard[]>([]);

  // ── Tabs del curso ────────────────────────────────────────────────────
  readonly cursoTabs: { id: TabCurso; label: string }[] = [
    { id: 'silabo',       label: 'Sílabo' },
    { id: 'contenido',    label: 'Contenido' },
    { id: 'evaluaciones', label: 'Evaluaciones' },
    { id: 'tareas',       label: 'Tareas' },
    { id: 'notas',        label: 'Notas' },
    { id: 'asistencia',   label: 'Asistencia' },
  ];

  // ── Bimestres (contenido del curso) ───────────────────────────────────
  readonly bimestres = signal<BimestreData[]>([]);

  notas: NotaAlumno[] = [];
  resumenAsistencia: ResumenAsistencia | null = null;
  historialAsistencia: Asistencia[] = [];
  horario: HorarioCurso[] = this.generarHorario();

  // ── Ciclo de vida ─────────────────────────────────────────────────────
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    const user = this.authService.getUsuarioActual();
    if (user && user.username) {
      this.alumnoService.buscarPorDni(user.username).subscribe({
        next: (alumno) => {
          this.alumnoId.set(alumno.id);
          this.alumnoService.obtenerCursos(alumno.id).subscribe({
            next: (data) => {
              const colors = ['#fde8d8', '#d4edfa', '#d9f5e0', '#ede8fd', '#fdf5d4'];
              const cards: CursoCard[] = data.map((ca, i) => {
                let iconKey = 'book';
                const area = ca.areaCurricular.toLowerCase();
                if (area.includes('matem')) iconKey = 'math';
                else if (area.includes('comunic')) iconKey = 'comm';
                else if (area.includes('cienc') || area.includes('cta')) iconKey = 'science';
                else if (area.includes('hist') || area.includes('geog')) iconKey = 'history';
                else if (area.includes('ingl')) iconKey = 'english';
                else if (area.includes('física') || area.includes('deport')) iconKey = 'sport';
                else if (area.includes('arte')) iconKey = 'art';
                else if (area.includes('tutor')) iconKey = 'tutor';

                let docenteNombre = 'Docente Asignado';
                if (ca.docente && ca.docente.nombres) {
                  docenteNombre = `${ca.docente.nombres.split(' ')[0]} ${ca.docente.apellidoPaterno}`;
                }

                return {
                  id: ca.id,
                  nombre: ca.areaCurricular,
                  codigo: `${ca.areaCurricular.substring(0,3).toUpperCase()}-${ca.aula.grado}${ca.aula.seccion}`,
                  grado: `${ca.aula.grado} ${ca.aula.nivel}`,
                  modalidad: 'Presencial',
                  docente: docenteNombre,
                  progreso: 0,
                  color: colors[i % colors.length],
                  icono: iconKey
                };
              });
              this.cursos.set(cards);
            }
          });
        },
        error: (err: any) => console.error('Error fetching alumno', err)
      });
    }
    this.cargarNotas();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Navegación ─────────────────────────────────────────────────────────
  setSidebarNav(nav: SidebarNav): void {
    this.sidebarNav.set(nav);
    if (nav === 'cursos') {
      this.cursosVista.set('lista');
      this.cursoActivo.set(null);
    }
  }

  abrirCurso(curso: CursoCard): void {
    this.cursoActivo.set(curso);
    this.cursosVista.set('detalle');
    this.tabCurso.set('contenido');

    this.cargando.set(true);
    this.materialService.listarPorCursoAsignado(curso.id).subscribe({
      next: (materiales: any[]) => {
        // Build bimestres with materiales
        const bimestres: BimestreData[] = [
          { numero: 1, nombre: 'Primer Bimestre', periodo: 'Mar – May', semanas: [] },
          { numero: 2, nombre: 'Segundo Bimestre', periodo: 'Jun – Jul', semanas: [] },
          { numero: 3, nombre: 'Tercer Bimestre', periodo: 'Ago – Sep', semanas: [] },
          { numero: 4, nombre: 'Cuarto Bimestre', periodo: 'Oct – Nov', semanas: [] },
        ];

        // Initialize 5 weeks per bimestre
        for (let b = 0; b < 4; b++) {
          for (let s = 1; s <= 5; s++) {
            bimestres[b].semanas.push({
              numero: b * 5 + s,
              actividades: []
            });
          }
        }

        // Fill materials
        materiales.forEach((m: any) => {
          const semana = m.semana;
          const bimestreIndex = Math.floor((semana - 1) / 5);
          if (bimestreIndex >= 0 && bimestreIndex < 4) {
            const semanaData = bimestres[bimestreIndex].semanas.find(s => s.numero === semana);
            if (semanaData) {
              semanaData.actividades.push({
                tipo: 'material',
                esCalificada: false,
                subtipo: 'Material de lectura',
                nombre: m.nombreArchivo,
                estado: 'no_revisado',
                desde: m.fechaSubida,
                hasta: '',
                url: m.urlArchivo
              });
            }
          }
        });

        this.bimestres.set(bimestres);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  volverALista(): void {
    this.cursosVista.set('lista');
    this.cursoActivo.set(null);
  }

  setTabCurso(tab: TabCurso): void {
    this.tabCurso.set(tab);
    if (tab === 'asistencia') this.cargarAsistencia();
  }

  // ── Acordeón ──────────────────────────────────────────────────────────
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

  // ── Helpers ────────────────────────────────────────────────────────────
  getEstadoLabel(estado: EstadoActividad): string {
    const mapa: Record<EstadoActividad, string> = {
      entregada: 'Entregada', vencida: 'Vencida',
      por_entregar: 'Por entregar', no_revisado: 'No revisado',
    };
    return mapa[estado];
  }

  getEstadoAsistenciaClass(estado: string): string {
    const mapa: Record<string, string> = {
      'ASISTIO': 'estado-asistio', 'FALTA': 'estado-falta',
      'TARDANZA': 'estado-tardanza', 'JUSTIFICADO': 'estado-justificado',
      'PERMISO_ACADEMIA': 'estado-academia', 'LICENCIA': 'estado-licencia'
    };
    return mapa[estado] ?? '';
  }

  formatearEstado(estado: string): string {
    const etiquetas: Record<string, string> = {
      'ASISTIO': 'Asistió', 'FALTA': 'Falta', 'TARDANZA': 'Tardanza',
      'JUSTIFICADO': 'Justificado', 'PERMISO_ACADEMIA': 'Permiso Academia', 'LICENCIA': 'Licencia'
    };
    return etiquetas[estado] ?? estado;
  }

  getLiteralClass(literal?: string): string {
    switch (literal) {
      case 'AD': return 'badge-ad'; case 'A': return 'badge-a';
      case 'B': return 'badge-b';   case 'C': return 'badge-c';
      default:   return '';
    }
  }

  getActividadesFiltradas(tipo: 'evaluacion' | 'tarea'): { bimestre: number; semana: number; act: ActividadBimestre }[] {
    const result: { bimestre: number; semana: number; act: ActividadBimestre }[] = [];
    for (const b of this.bimestres()) {
      for (const s of b.semanas) {
        for (const a of s.actividades) {
          if (a.tipo === tipo) result.push({ bimestre: b.numero, semana: s.numero, act: a });
        }
      }
    }
    return result;
  }

  tieneTareas(sem: SemanaData): boolean {
    return sem.actividades.some(a => a.tipo === 'tarea');
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
      { area: 'Tutoría',             b1: 18, b2: 18, b3: 18, b4: 18, promedio: 18.0, literal: 'AD' },
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
      { hora: '07:30 – 08:15', lunes: 'Matemática',  martes: 'Comunicación', miercoles: 'CTA',       jueves: 'Historia',    viernes: 'Inglés'     },
      { hora: '08:15 – 09:00', lunes: 'Matemática',  martes: 'Comunicación', miercoles: 'CTA',       jueves: 'Historia',    viernes: 'Inglés'     },
      { hora: '09:00 – 09:45', lunes: 'Inglés',      martes: 'Matemática',   miercoles: 'Comunicación', jueves: 'Arte',     viernes: 'Ed. Física' },
      { hora: '09:45 – 10:00', lunes: '— Recreo —',  martes: '— Recreo —',  miercoles: '— Recreo —', jueves: '— Recreo —', viernes: '— Recreo —' },
      { hora: '10:00 – 10:45', lunes: 'Historia',    martes: 'CTA',          miercoles: 'Matemática', jueves: 'Comunicación', viernes: 'Tutoría' },
      { hora: '10:45 – 11:30', lunes: 'Historia',    martes: 'CTA',          miercoles: 'Matemática', jueves: 'Comunicación', viernes: 'Tutoría' },
      { hora: '11:30 – 12:15', lunes: 'Arte',        martes: 'Ed. Física',   miercoles: 'Historia',  jueves: 'CTA',          viernes: 'Matemática' },
      { hora: '12:15 – 13:00', lunes: 'Arte',        martes: 'Ed. Física',   miercoles: 'Historia',  jueves: 'CTA',          viernes: 'Matemática' },
    ];
  }
}
