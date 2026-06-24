import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../core/services/auth.service';
import { SyncService } from '../../core/services/sync.service';
import {
  NotaService,
  FilaNota,
  ESCALA_LITERAL_OPCIONES,
  EscalaLiteral
} from '../../core/services/nota.service';

export type TabCurso = 'contenido' | 'tareas' | 'notas' | 'asistencia';
export type SidebarNav = 'cursos' | 'asistencia' | 'notas' | 'configuracion';
export type CursosVista = 'lista' | 'detalle';
export type EstadoAsistencia = 'ASISTIO' | 'FALTA' | 'TARDANZA' | 'JUSTIFICADO';

export interface CursoCard {
  id: number;
  nombre: string;
  codigo: string;
  grado: string;
  seccion: string;
  modalidad: 'Presencial' | 'Virtual';
  color: string;
  icono: string;
  estudiantes: number;
  bimestreActual: number;
  semanaActual: number;
  totalSemanas: number;
  horario: string;
}

export interface AlumnoMock {
  id: number;
  nombre: string;
  codigo: string;
  permisoAcademia: boolean;
  horarioEspecial?: string;
  nee: boolean;
  estado: EstadoAsistencia;
}

export interface TareaItem {
  id: string;
  tipo: 'tarea' | 'avance';
  titulo: string;
  descripcion: string;
  fechaLimite: string;
  puntaje: number;
}

export interface SemanaData {
  numero: number;
  label: string;
  temas: string[];
  asistenciaRegistrada: boolean;
  tareas: TareaItem[];
}

export interface BimestreData {
  numero: number;
  label: string;
  semanas: SemanaData[];
}

@Component({
  selector: 'app-docente-portal',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './docente-portal.component.html',
  styleUrls: ['./docente-portal.component.scss']
})
export class DocentePortalComponent implements OnInit {

  public authService = inject(AuthService);
  private notaService = inject(NotaService);
  public syncService = inject(SyncService);

  readonly isOnline = toSignal(this.syncService.isOnline$, { initialValue: navigator.onLine });
  readonly pendingCount = toSignal(this.syncService.pendingCount$, { initialValue: 0 });

  readonly escalaLiteralOpciones = ESCALA_LITERAL_OPCIONES;

  readonly sidebarNav = signal<SidebarNav>('cursos');
  readonly cursosVista = signal<CursosVista>('lista');
  readonly tabCurso = signal<TabCurso>('contenido');
  readonly cursoActivo = signal<CursoCard | null>(null);
  readonly anioActual = signal(new Date().getFullYear());

  readonly modalAsistenciaVisible = signal(false);
  readonly modalTareaVisible = signal(false);
  readonly semanaSeleccionada = signal<SemanaData | null>(null);

  searchQuery = '';

  private bimestresExpandidos = signal<number[]>([1]);

  readonly usuarioNombre = computed(() =>
    this.authService.getUsuarioActual()?.username ?? 'Docente'
  );

  readonly usuarioInicial = computed(() =>
    (this.authService.getUsuarioActual()?.username ?? 'D')[0]?.toUpperCase() ?? 'D'
  );

  readonly cursos: CursoCard[] = [
    {
      id: 1, nombre: 'Matemática', codigo: 'MAT-5A', grado: '5to Primaria',
      seccion: '5°A', modalidad: 'Presencial', color: '#2563EB',
      icono: 'math', estudiantes: 28, bimestreActual: 2,
      semanaActual: 5, totalSemanas: 20, horario: 'Lun, Mié, Vie 8:00 - 9:00'
    },
    {
      id: 2, nombre: 'Comunicación', codigo: 'COM-5A', grado: '5to Primaria',
      seccion: '5°A', modalidad: 'Presencial', color: '#10B981',
      icono: 'comm', estudiantes: 28, bimestreActual: 2,
      semanaActual: 5, totalSemanas: 20, horario: 'Mar, Jue 8:00 - 9:30'
    },
    {
      id: 3, nombre: 'Ciencia y Tecnología', codigo: 'CYT-4B', grado: '4to Primaria',
      seccion: '4°B', modalidad: 'Presencial', color: '#7C3AED',
      icono: 'science', estudiantes: 25, bimestreActual: 2,
      semanaActual: 4, totalSemanas: 20, horario: 'Lun, Mié 10:00 - 11:00'
    },
    {
      id: 4, nombre: 'Personal Social', codigo: 'PS-4B', grado: '4to Primaria',
      seccion: '4°B', modalidad: 'Presencial', color: '#F59E0B',
      icono: 'default', estudiantes: 25, bimestreActual: 2,
      semanaActual: 4, totalSemanas: 20, horario: 'Mar, Jue 10:00 - 11:30'
    },
  ];

  readonly cursoTabs: { id: TabCurso; label: string }[] = [
    { id: 'contenido',  label: 'Contenido' },
    { id: 'tareas',     label: 'Tareas' },
    { id: 'notas',      label: 'Calificaciones' },
    { id: 'asistencia', label: 'Asistencia' },
  ];

  bimestres: BimestreData[] = [];

  // ── Asistencia ────────────────────────────────────────────────────────────
  alumnosMock: AlumnoMock[] = [];
  asistenciaStats = { presentes: 0, tardanzas: 0, faltas: 0, justificados: 0 };

  // ── Tarea form ────────────────────────────────────────────────────────────
  nuevaTarea = {
    tipo: 'tarea' as 'tarea' | 'avance',
    titulo: '',
    descripcion: '',
    fechaLimite: '',
    puntaje: 10
  };

  // ── Notas ─────────────────────────────────────────────────────────────────
  filasNotas: FilaNota[] = [];
  bimestreActivo = 1;
  guardandoNotas = signal(false);
  notasGuardadasExito = signal(false);
  errorNotasSinEvidencia = signal(false);

  // ── Computeds ─────────────────────────────────────────────────────────────
  get cursosFiltrados(): CursoCard[] {
    const q = this.searchQuery.toLowerCase().trim();
    if (!q) return this.cursos;
    return this.cursos.filter(c =>
      c.nombre.toLowerCase().includes(q) ||
      c.grado.toLowerCase().includes(q) ||
      c.codigo.toLowerCase().includes(q)
    );
  }

  get hayAlumnosConPermiso(): boolean {
    return this.alumnosMock.some(a => a.permisoAcademia);
  }

  get progresoSemanas(): number {
    const curso = this.cursoActivo();
    if (!curso) return 0;
    return Math.round((curso.semanaActual / curso.totalSemanas) * 100);
  }

  get bimestreLabel(): string {
    const labels = ['', 'Primer', 'Segundo', 'Tercer', 'Cuarto'];
    const cur = this.cursoActivo();
    return cur ? (labels[cur.bimestreActual] ?? '') + ' Bimestre' : '';
  }

  ngOnInit(): void {
    this.bimestres = this.buildBimestres();
  }

  // ── Navegación ────────────────────────────────────────────────────────────

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
    this.bimestresExpandidos.set([curso.bimestreActual]);
    this.cargarFilasNotas();
  }

  volverALista(): void {
    this.cursosVista.set('lista');
    this.cursoActivo.set(null);
  }

  setTabCurso(tab: TabCurso): void {
    this.tabCurso.set(tab);
  }

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

  // ── Modales ───────────────────────────────────────────────────────────────

  async abrirModalAsistencia(semana: SemanaData): Promise<void> {
    this.semanaSeleccionada.set(semana);
    const aulaId = this.cursoActivo()?.id ?? 1;

    let cache = await this.syncService.getAlumnosCache(aulaId);
    if (!cache || cache.length === 0) {
      cache = [
        { id: 1,  nombre: 'Alarcón Huanca, Rosa',       codigo: '2026001', permisoAcademia: false, nee: false,  estado: 'ASISTIO' },
        { id: 2,  nombre: 'Cárdenas López, Miguel',     codigo: '2026002', permisoAcademia: false, nee: false,  estado: 'ASISTIO' },
        { id: 3,  nombre: 'Chávez Quispe, Lucía',       codigo: '2026003', permisoAcademia: true,  nee: true,   horarioEspecial: '2:30 PM', estado: 'ASISTIO' },
        { id: 4,  nombre: 'Flores Torres, Andrés',      codigo: '2026004', permisoAcademia: false, nee: false,  estado: 'ASISTIO' },
        { id: 5,  nombre: 'García Mamani, Sofía',       codigo: '2026005', permisoAcademia: false, nee: true,   estado: 'ASISTIO' },
        { id: 6,  nombre: 'Huanca Ramos, Diego',        codigo: '2026006', permisoAcademia: false, nee: false,  estado: 'ASISTIO' },
        { id: 7,  nombre: 'Jiménez Soto, Valentina',    codigo: '2026007', permisoAcademia: true,  nee: false,  horarioEspecial: '1:30 PM', estado: 'ASISTIO' },
        { id: 8,  nombre: 'López Cruz, Sebastián',      codigo: '2026008', permisoAcademia: false, nee: false,  estado: 'ASISTIO' },
        { id: 9,  nombre: 'Mamani Condori, Camila',     codigo: '2026009', permisoAcademia: false, nee: false,  estado: 'ASISTIO' },
        { id: 10, nombre: 'Quispe Vargas, Mateo',       codigo: '2026010', permisoAcademia: false, nee: false,  estado: 'ASISTIO' },
        { id: 11, nombre: 'Ramos Ticona, Isabella',     codigo: '2026011', permisoAcademia: false, nee: false,  estado: 'ASISTIO' },
        { id: 12, nombre: 'Rivera Puma, Emilio',        codigo: '2026012', permisoAcademia: false, nee: false,  estado: 'ASISTIO' },
      ];
      await this.syncService.saveAlumnosCache(aulaId, cache);
    }
    
    this.alumnosMock = cache;
    this.recalcularStats();
    this.modalAsistenciaVisible.set(true);
  }

  abrirModalTarea(semana: SemanaData): void {
    this.semanaSeleccionada.set(semana);
    this.nuevaTarea = { tipo: 'tarea', titulo: '', descripcion: '', fechaLimite: '', puntaje: 10 };
    this.modalTareaVisible.set(true);
  }

  cerrarModales(): void {
    this.modalAsistenciaVisible.set(false);
    this.modalTareaVisible.set(false);
  }

  setEstadoAlumno(alumno: AlumnoMock, estado: EstadoAsistencia): void {
    alumno.estado = estado;
    this.recalcularStats();
  }

  marcarTodos(estado: EstadoAsistencia): void {
    this.alumnosMock.forEach(a => a.estado = estado);
    this.recalcularStats();
  }

  recalcularStats(): void {
    this.asistenciaStats = {
      presentes:    this.alumnosMock.filter(a => a.estado === 'ASISTIO').length,
      tardanzas:    this.alumnosMock.filter(a => a.estado === 'TARDANZA').length,
      faltas:       this.alumnosMock.filter(a => a.estado === 'FALTA').length,
      justificados: this.alumnosMock.filter(a => a.estado === 'JUSTIFICADO').length,
    };
  }

  async guardarAsistencia(): Promise<void> {
    const semana = this.semanaSeleccionada();
    if (semana) semana.asistenciaRegistrada = true;
    
    const aulaId = this.cursoActivo()?.id ?? 1;
    const docenteId = 1; // Simulate authService ID
    
    const registros: any[] = this.alumnosMock.map(a => ({
      alumnoId: a.id,
      estado: a.estado,
      justificacion: ''
    }));

    if (!this.isOnline()) {
      console.log('Modo OFFLINE: Guardando asistencia en IndexedDB...', registros);
      await this.syncService.queueAsistencia(aulaId, new Date().toISOString().split('T')[0], docenteId, registros);
    } else {
      console.log('Modo ONLINE: Guardando asistencia en backend...', registros);
      // Aqui iria AsistenciaService.registrarAsistenciaLote
    }

    this.cerrarModales();
  }

  guardarTarea(): void {
    const semana = this.semanaSeleccionada();
    if (semana && this.nuevaTarea.titulo.trim()) {
      semana.tareas.push({
        id: 't_' + Date.now(),
        tipo: this.nuevaTarea.tipo,
        titulo: this.nuevaTarea.titulo,
        descripcion: this.nuevaTarea.descripcion,
        fechaLimite: this.nuevaTarea.fechaLimite,
        puntaje: this.nuevaTarea.puntaje
      });
    }
    this.cerrarModales();
  }

  // ── Notas ─────────────────────────────────────────────────────────────────

  cargarFilasNotas(): void {
    const curso = this.cursoActivo();
    if (!curso) return;
    const alumnosMock = [
      { id: 101, nombre: 'QUISPE MAMANI, Juan' },
      { id: 102, nombre: 'GARCÍA SILVA, Ana María' },
      { id: 103, nombre: 'CONDORI LÓPEZ, Luis' },
      { id: 104, nombre: 'TORRES CRUZ, Carlos' },
      { id: 105, nombre: 'ROJAS PEÑA, Sofía' },
    ];
    this.filasNotas = this.notaService.construirFilasSimuladas(alumnosMock, curso.grado);
  }

  onNotaChange(fila: FilaNota): void { fila.modificado = true; }

  onArchivoSeleccionado(evento: Event, fila: FilaNota): void {
    const input = evento.target as HTMLInputElement;
    if (!input.files?.length) return;
    fila.archivoPendiente = input.files[0];
    fila.conEvidencia     = true;
    fila.modificado       = true;
  }

  notaLiteralValida(fila: FilaNota): boolean {
    return fila.tipoEscala === 'LITERAL' && !!fila.notaLiteral;
  }

  notaVigesimalValida(fila: FilaNota): boolean {
    return fila.tipoEscala === 'VIGESIMAL' &&
      this.notaService.validarVigesimal(fila.notaVigesimal);
  }

  guardarNotas(): void {
    const sinEvidencia = this.filasNotas.some(f => f.modificado && !f.conEvidencia);
    if (sinEvidencia) {
      this.errorNotasSinEvidencia.set(true);
      setTimeout(() => this.errorNotasSinEvidencia.set(false), 4000);
      return;
    }
    this.guardandoNotas.set(true);
    this.errorNotasSinEvidencia.set(false);
    setTimeout(() => {
      this.guardandoNotas.set(false);
      this.notasGuardadasExito.set(true);
      setTimeout(() => this.notasGuardadasExito.set(false), 3000);
    }, 800);
  }

  // ── Bimestres (datos) ─────────────────────────────────────────────────────

  private buildBimestres(): BimestreData[] {
    return [
      {
        numero: 1, label: 'Primer Bimestre',
        semanas: [
          { numero: 1,  label: 'Semana 01', temas: ['Presentación del curso', 'Diagnóstico inicial', 'Acuerdos de convivencia'], asistenciaRegistrada: true,  tareas: [{ id: 't1', tipo: 'tarea',  titulo: 'Ficha de diagnóstico',  descripcion: 'Completar la ficha de exploración de conocimientos previos.', fechaLimite: '14 Mar 2026', puntaje: 5 }] },
          { numero: 2,  label: 'Semana 02', temas: ['Unidad 1 – Tema 1', 'Actividades de exploración', 'Ejercicios prácticos'], asistenciaRegistrada: true,  tareas: [] },
          { numero: 3,  label: 'Semana 03', temas: ['Unidad 1 – Tema 2', 'Resolución de problemas'], asistenciaRegistrada: true,  tareas: [{ id: 't2', tipo: 'avance', titulo: 'Avance Proyecto 1', descripcion: 'Presentar esquema inicial del proyecto integrador.', fechaLimite: '28 Mar 2026', puntaje: 10 }] },
          { numero: 4,  label: 'Semana 04', temas: ['Consolidación Unidad 1', 'Evaluación formativa'], asistenciaRegistrada: true,  tareas: [] },
          { numero: 5,  label: 'Semana 05', temas: ['Inicio Unidad 2', 'Nuevos conceptos clave'], asistenciaRegistrada: false, tareas: [] },
        ]
      },
      {
        numero: 2, label: 'Segundo Bimestre',
        semanas: [
          { numero: 6,  label: 'Semana 06', temas: ['Repaso general', 'Retroalimentación bimestre anterior'], asistenciaRegistrada: true,  tareas: [] },
          { numero: 7,  label: 'Semana 07', temas: ['Unidad 3 – Introducción', 'Material de lectura'], asistenciaRegistrada: true,  tareas: [] },
          { numero: 8,  label: 'Semana 08', temas: ['Unidad 3 – Desarrollo', 'Trabajo colaborativo'], asistenciaRegistrada: false, tareas: [] },
          { numero: 9,  label: 'Semana 09', temas: ['Aplicación práctica', 'Resolución grupal'], asistenciaRegistrada: false, tareas: [] },
          { numero: 10, label: 'Semana 10', temas: ['Evaluación de proceso', 'Cierre parcial'], asistenciaRegistrada: false, tareas: [] },
        ]
      },
      {
        numero: 3, label: 'Tercer Bimestre',
        semanas: [
          { numero: 11, label: 'Semana 11', temas: ['Unidad 4 – Apertura'], asistenciaRegistrada: false, tareas: [] },
          { numero: 12, label: 'Semana 12', temas: ['Desarrollo conceptual'], asistenciaRegistrada: false, tareas: [] },
          { numero: 13, label: 'Semana 13', temas: ['Talleres de profundización'], asistenciaRegistrada: false, tareas: [] },
          { numero: 14, label: 'Semana 14', temas: ['Evaluación bimestral'], asistenciaRegistrada: false, tareas: [] },
          { numero: 15, label: 'Semana 15', temas: ['Retroalimentación y mejora'], asistenciaRegistrada: false, tareas: [] },
        ]
      },
      {
        numero: 4, label: 'Cuarto Bimestre',
        semanas: [
          { numero: 16, label: 'Semana 16', temas: ['Proyecto final – Inicio'], asistenciaRegistrada: false, tareas: [] },
          { numero: 17, label: 'Semana 17', temas: ['Proyecto final – Desarrollo'], asistenciaRegistrada: false, tareas: [] },
          { numero: 18, label: 'Semana 18', temas: ['Exposición de proyectos'], asistenciaRegistrada: false, tareas: [] },
          { numero: 19, label: 'Semana 19', temas: ['Evaluación final'], asistenciaRegistrada: false, tareas: [] },
          { numero: 20, label: 'Semana 20', temas: ['Clausura y entrega de libretas'], asistenciaRegistrada: false, tareas: [] },
        ]
      }
    ];
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  esSemanaActual(semana: SemanaData): boolean {
    return semana.numero === (this.cursoActivo()?.semanaActual ?? -1);
  }

  colorForCurso(color: string): string {
    return color;
  }

  colorLight(hex: string): string {
    return hex + '1a'; // 10% opacity
  }
}
