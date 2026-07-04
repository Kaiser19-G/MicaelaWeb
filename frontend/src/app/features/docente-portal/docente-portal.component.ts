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
import { DocenteService } from '../../core/services/docente.service';
import { CalificacionService } from '../../core/services/calificacion.service';
import { MaterialService } from '../../core/services/material.service';
import { AlumnoService } from '../../core/services/alumno.service';
import { AsistenciaService } from '../../core/services/asistencia.service';
import { TareaService } from '../../core/services/tarea.service';

export type TabCurso = 'contenido' | 'tareas' | 'notas' | 'asistencia';
export type SidebarNav = 'cursos' | 'asistencia' | 'notas' | 'configuracion' | 'horario';
export type CursosVista = 'lista' | 'detalle';

export interface Tarea {
  id: string;
  tipo: 'tarea' | 'avance';
  titulo: string;
  descripcion: string;
  fechaLimite: string;
  puntaje: number;
  tipoCalificacion?: string;
}

export interface SemanaData {
  numero: number;
  label: string;
  temas: string[];
  asistenciaRegistrada: boolean;
  calificacionDiariaRegistrada?: boolean;
  tareas: Tarea[];
  recursos?: any[];
}

export interface CursoCard {
  id: number;
  aulaId?: number;
  nombre: string;
  codigo: string;
  grado: string;
  seccion: string;
  modalidad: string;
  color: string;
  icono: string;
  estudiantes: number;
  bimestreActual: number;
  semanaActual: number;
  totalSemanas: number;
  horario: string;
}

export type EstadoAsistencia = 'ASISTIO' | 'TARDANZA' | 'FALTA' | 'JUSTIFICADO' | 'TARDANZA_JUSTIFICADA';

export interface AlumnoMock {
  id: number;
  nombre: string;
  codigo: string;
  permisoAcademia: boolean;
  nee: boolean;
  horarioEspecial?: string;
  estado: EstadoAsistencia;
  calificacionDiaria?: string;
}

export interface TareaItem {
  id: string;
  tipo: 'tarea' | 'avance';
  titulo: string;
  descripcion: string;
  fechaLimite: string;
  puntaje: number;
}

// removed duplicate

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
  private docenteService = inject(DocenteService);
  private calificacionService = inject(CalificacionService);
  private materialService = inject(MaterialService);
  private alumnoService = inject(AlumnoService);
  private asistenciaService = inject(AsistenciaService);
  private tareaService = inject(TareaService);

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
  readonly modalCalificacionVisible = signal(false);
  readonly modalExitoVisible = signal(false);
  readonly mensajeExito = signal("");
  readonly esError = signal(false);
  readonly semanaSeleccionada = signal<SemanaData | null>(null);

  searchQuery = '';

  private bimestresExpandidos = signal<number[]>([1]);

  readonly usuarioNombre = computed(() =>
    this.authService.getUsuarioActual()?.username ?? 'Docente'
  );

  readonly usuarioInicial = computed(() =>
    (this.authService.getUsuarioActual()?.username ?? 'D')[0]?.toUpperCase() ?? 'D'
  );

  readonly cursos = signal<CursoCard[]>([]);

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
  nuevaTarea: Tarea = {
    id: '',
    tipo: 'tarea' as 'tarea' | 'avance',
    titulo: '',
    descripcion: '',
    fechaLimite: '',
    puntaje: 10,
    tipoCalificacion: 'numeros'
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
    if (!q) return this.cursos();
    return this.cursos().filter(c =>
      c.nombre.toLowerCase().includes(q) ||
      c.grado.toLowerCase().includes(q) ||
      c.seccion.toLowerCase().includes(q)
    );
  }

  get hayAlumnosConPermiso(): boolean {
    return this.alumnosMock.some(a => a.permisoAcademia);
  }

  get totalEstudiantes(): number {
    return this.cursos().reduce((acc, c) => acc + c.estudiantes, 0);
  }

  get progresoGeneral(): number {
    const cur = this.cursoActivo();
    if (!cur) return 0;
    return Math.round((cur.semanaActual / cur.totalSemanas) * 100);
  }

  get bimestreLabel(): string {
    const labels = ['', 'Primer', 'Segundo', 'Tercer', 'Cuarto'];
    const cur = this.cursoActivo();
    return cur ? (labels[cur.bimestreActual] ?? '') + ' Bimestre' : '';
  }

  cargandoCursos = signal<boolean>(false);

  ngOnInit(): void {
    this.bimestres = this.buildBimestres();
    this.cargarCursosDocente();
  }
  
  cargarCursosDocente(): void {
    const usuario = this.authService.getUsuarioActual();
    if (usuario && usuario.id) {
      this.cargandoCursos.set(true);
      this.docenteService.obtenerCursos(usuario.id).subscribe({
        next: (cursosAsignados: any[]) => {
          const colors = ['#2563EB', '#10B981', '#7C3AED', '#F59E0B', '#EF4444'];
          const mappedCursos: CursoCard[] = cursosAsignados.map((ca, i) => ({
            id: ca.id,
            aulaId: ca.aula.id,
            nombre: ca.areaCurricular,
            codigo: `${ca.areaCurricular.substring(0,3).toUpperCase()}-${ca.aula.grado[0]}${ca.aula.seccion}`,
            grado: `${ca.aula.grado} ${ca.aula.nivel}`,
            seccion: `${ca.aula.grado[0]}°${ca.aula.seccion}`,
            modalidad: 'Presencial',
            color: colors[i % colors.length],
            icono: this.getIconForArea(ca.areaCurricular),
            estudiantes: 0,
            bimestreActual: 2,
            semanaActual: 5,
            totalSemanas: 20,
            horario: 'Por definir'
          }));
          this.cursos.set(mappedCursos);
          this.cargandoCursos.set(false);
        },
        error: (err: any) => {
          console.error('Error al cargar cursos', err);
          this.cargandoCursos.set(false);
        }
      });
    }
  }

  getIconForArea(area: string): string {
    const a = area.toLowerCase();
    if (a.includes('matem')) return 'math';
    if (a.includes('comunic')) return 'comm';
    if (a.includes('cienc')) return 'science';
    if (a.includes('ingl')) return 'language';
    return 'default';
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
    
    this.alumnoService.listarPorAula(curso.aulaId ?? 1).subscribe({
      next: (alumnos) => {
        this.alumnosMock = alumnos.map(a => ({
          id: a.id,
          nombre: a.nombreCompleto,
          codigo: a.codigoEstudiante,
          permisoAcademia: a.tienePermisoAcademia,
          nee: false,
          estado: 'FALTA' as EstadoAsistencia
        }));
        curso.estudiantes = alumnos.length;
        this.cargarFilasNotas();
      }
    });

    // Cargar tareas persistidas del backend para todas las semanas
    this.cargarTareasYMateriales(curso.id);
  }

  cargarTareasYMateriales(cursoAsignadoId: number): void {
    // Reconstruir bimestres frescos primero
    this.bimestres = this.buildBimestres();
    // Cargar tareas semana por semana
    this.bimestres.forEach(bim => {
      bim.semanas.forEach(sem => {
        this.tareaService.obtenerTareasPorCursoYSemana(cursoAsignadoId, sem.numero).subscribe({
          next: (tareas: any[]) => {
            sem.tareas = tareas.map(t => ({
              id: t.id?.toString() || '',
              tipo: 'tarea' as 'tarea' | 'avance',
              titulo: t.titulo,
              descripcion: t.descripcion || '',
              fechaLimite: t.fechaLimite ? t.fechaLimite.split('T')[0] : '',
              puntaje: 10
            }));
          },
          error: () => {} // Silenciar errores individuales de semana
        });

        this.materialService.obtenerMateriales(cursoAsignadoId, sem.numero).subscribe({
          next: (materiales: any[]) => {
            sem.recursos = materiales;
          },
          error: () => {}
        });
      });
    });
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

  cerrarModales(): void {
    this.modalAsistenciaVisible.set(false);
    this.modalTareaVisible.set(false);
    this.modalCalificacionVisible.set(false);
  }

  mostrarExito(mensaje: string, error: boolean = false): void {
    this.esError.set(error);
    this.mensajeExito.set(mensaje);
    this.modalExitoVisible.set(true);
    setTimeout(() => {
      this.modalExitoVisible.set(false);
      if (!error) this.cerrarModales();
    }, 3000);
  }

  abrirModalAsistencia(semana: SemanaData): void {
    this.semanaSeleccionada.set(semana);
    const aulaId = this.cursoActivo()?.aulaId ?? 1;
    const fechaSemana = this.fechaDeSemana(semana.numero);

    this.asistenciaService.obtenerAsistenciaAula(aulaId, fechaSemana).subscribe({
      next: (asistencias) => {
        if (asistencias.length > 0) {
          semana.asistenciaRegistrada = true;
          this.alumnosMock.forEach(a => {
            const asis = asistencias.find((reg: any) => reg.alumno.id === a.id);
            if (asis) {
              a.estado = asis.estado as any;
            }
          });
        } else {
          semana.asistenciaRegistrada = false;
          this.alumnosMock.forEach(a => a.estado = 'FALTA' as any);
        }
        this.recalcularStats();
        this.modalAsistenciaVisible.set(true);
      },
      error: () => {
        this.alumnosMock.forEach(a => a.estado = 'FALTA' as any);
        this.recalcularStats();
        this.modalAsistenciaVisible.set(true);
      }
    });
  }

  abrirModalTarea(semana: SemanaData): void {
    this.semanaSeleccionada.set(semana);
    this.nuevaTarea = { 
      id: '', 
      tipo: 'tarea', 
      titulo: '', 
      descripcion: '', 
      fechaLimite: '', 
      puntaje: 10,
      tipoCalificacion: 'numeros' 
    };
    this.modalTareaVisible.set(true);
  }

  abrirModalCalificacion(semana: SemanaData): void {
    if (!semana.asistenciaRegistrada) {
      this.mostrarExito('Error: Primero tome la asistencia de los alumnos para calificarlos.', true);
      return;
    }
    this.semanaSeleccionada.set(semana);
    this.alumnosMock.forEach(a => {
      if (a.estado === 'FALTA') {
        a.calificacionDiaria = 'NP';
      } else {
        a.calificacionDiaria = '';
      }
    });
    this.modalCalificacionVisible.set(true);
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

  /** Calcula la fecha real (lunes de esa semana del año académico actual). */
  private fechaDeSemana(numeroSemana: number): string {
    // El año escolar en Perú empieza la primera semana de marzo
    const inicioAnioEscolar = new Date(new Date().getFullYear(), 2, 3); // 3 de marzo
    const diasOffset = (numeroSemana - 1) * 7;
    const fecha = new Date(inicioAnioEscolar.getTime() + diasOffset * 24 * 60 * 60 * 1000);
    return fecha.toISOString().split('T')[0];
  }

  /** Retorna el id del perfil (Docente) del usuario autenticado. */
  private getDocenteId(): number {
    const u = this.authService.getUsuarioActual();
    return u?.perfilId ?? u?.id ?? 1;
  }

  guardandoAsistencia = signal<boolean>(false);
  guardarAsistencia(): void {
    const semana = this.semanaSeleccionada();
    const aulaId = this.cursoActivo()?.aulaId ?? 1;
    const docenteId = this.getDocenteId();
    const fechaSemana = this.fechaDeSemana(semana?.numero || 1);
    
    const asistencias = this.alumnosMock.map(a => ({
      alumnoId: a.id,
      estado: a.estado as any,
      observacion: ''
    }));

    this.guardandoAsistencia.set(true);
    this.asistenciaService.registrarAsistenciaLote(aulaId, fechaSemana, docenteId, asistencias).subscribe({
      next: () => {
        if (semana) semana.asistenciaRegistrada = true;
        this.guardandoAsistencia.set(false);
        this.mostrarExito('Asistencia registrada correctamente');
      },
      error: (err: any) => {
        console.error('Error al guardar asistencias:', err);
        this.guardandoAsistencia.set(false);
        this.mostrarExito('Error: Hubo un problema al guardar la asistencia', true);
      }
    });
  }

  guardandoCalificacionDiaria = signal<boolean>(false);
  opcionesCalificacionDiaria = ['AD', 'A', 'B', 'C', 'NP'];
  
  setCalificacionDiaria(alumno: AlumnoMock, calificacion: string): void {
    alumno.calificacionDiaria = calificacion;
  }
  
  guardarCalificacionDiaria(): void {
    const semana = this.semanaSeleccionada();
    if (!semana) return;
    
    const cursoAsignadoId = this.cursoActivo()?.id ?? 1;
    const docenteId = this.getDocenteId();
    
    const calificaciones = this.alumnosMock
      .filter(a => a.calificacionDiaria)
      .map(a => ({
        alumnoId: a.id,
        calificacion: a.calificacionDiaria!
      }));
      
    this.guardandoCalificacionDiaria.set(true);
    this.calificacionService.guardarLote(cursoAsignadoId, semana.numero, docenteId, calificaciones).subscribe({
      next: () => {
        semana.calificacionDiariaRegistrada = true;
        this.guardandoCalificacionDiaria.set(false);
        this.mostrarExito('Calificaciones registradas correctamente');
      },
      error: (err: any) => {
        console.error('Error guardando calificaciones diarias', err);
        this.guardandoCalificacionDiaria.set(false);
        this.mostrarExito('Error: Hubo un problema al guardar las calificaciones', true);
      }
    });
  }

  guardarTarea(): void {
    const semana = this.semanaSeleccionada();
    if (semana && this.nuevaTarea.titulo.trim()) {
      const cursoId = this.cursoActivo()?.id ?? 1;
      const docenteId = this.getDocenteId();
      // Backend espera ISO DATE_TIME: añadimos T00:00:00 si solo es fecha
      let fechaLimite = this.nuevaTarea.fechaLimite;
      if (fechaLimite && !fechaLimite.includes('T')) {
        fechaLimite = fechaLimite + 'T23:59:59';
      }
      this.tareaService.crearTarea(
        cursoId, semana.numero, this.nuevaTarea.titulo, this.nuevaTarea.descripcion,
        fechaLimite, docenteId
      ).subscribe({
        next: (res: any) => {
          semana.tareas.push({
            id: res.id?.toString() || 't_' + Date.now(),
            tipo: this.nuevaTarea.tipo,
            titulo: this.nuevaTarea.titulo,
            descripcion: this.nuevaTarea.descripcion,
            fechaLimite: this.nuevaTarea.fechaLimite,
            puntaje: this.nuevaTarea.puntaje
          });
          this.mostrarExito('Tarea guardada correctamente');
        },
        error: (err: any) => {
          console.error('Error al guardar tarea', err);
          this.mostrarExito('Error al guardar la tarea', true);
        }
      });
    }
  }

  validarTamanioMaterial(event: Event, semana: SemanaData): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (file.size > 20 * 1024 * 1024) {
        this.mostrarExito('Error: El archivo excede el tamaño máximo permitido de 20MB', true);
        input.value = '';
      } else {
        const cursoId = this.cursoActivo()?.id ?? 1;
        const docenteId = this.getDocenteId();
        this.materialService.subirMaterial(cursoId, semana.numero, docenteId, file).subscribe({
          next: () => {
            this.mostrarExito(`Material subido correctamente a la Semana ${semana.numero}`);
            input.value = '';
          },
          error: (err: any) => {
            console.error('Error al subir material', err);
            this.mostrarExito('Error: Hubo un problema al subir el material', true);
          }
        });
      }
    }
  }

  getUrlDescarga(material: any): string {
    return material?.urlArchivo ?? material?.url ?? '';
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
