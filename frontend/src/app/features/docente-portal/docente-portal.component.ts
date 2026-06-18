import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../core/services/auth.service';
import {
  NotaService,
  FilaNota,
  ESCALA_LITERAL_OPCIONES,
  EscalaLiteral
} from '../../core/services/nota.service';

export type TabCurso = 'silabo' | 'contenido' | 'evaluaciones' | 'tareas' | 'notas' | 'asistencia';
export type SidebarNav = 'cursos' | 'asistencia' | 'notas' | 'configuracion';
export type CursosVista = 'lista' | 'detalle';

export interface CursoCard {
  id: number;
  nombre: string;
  codigo: string;
  grado: string;
  modalidad: 'Presencial' | 'Virtual';
  color: string;
  icono: string;
}

export interface AlumnoMock {
  id: number;
  nombre: string;
  permisoAcademia: boolean;
  estado: string; // 'ASISTIO', 'FALTA', 'TARDANZA'
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

  // Exponemos la constante al template
  readonly escalaLiteralOpciones = ESCALA_LITERAL_OPCIONES;

  readonly sidebarNav = signal<SidebarNav>('cursos');
  readonly cursosVista = signal<CursosVista>('lista');
  readonly tabCurso = signal<TabCurso>('contenido');
  readonly cursoActivo = signal<CursoCard | null>(null);
  readonly anioActual = signal(new Date().getFullYear());
  
  readonly modalAsistenciaVisible = signal(false);
  readonly modalTareaVisible = signal(false);
  readonly semanaSeleccionada = signal(1);

  private bimestresExpandidos = signal<number[]>([1]);

  readonly usuarioNombre = computed(() =>
    this.authService.getUsuarioActual()?.username ?? 'Docente'
  );
  
  readonly usuarioInicial = computed(() =>
    (this.authService.getUsuarioActual()?.username ?? 'D')[0]?.toUpperCase() ?? 'D'
  );

  readonly cursos: CursoCard[] = [
    { id: 1, nombre: 'Matemática', codigo: '5to-A', grado: '5to Secundaria - Secc. A', modalidad: 'Presencial', color: '#fde8d8', icono: 'math' },
    { id: 2, nombre: 'Matemática', codigo: '5to-B', grado: '5to Secundaria - Secc. B', modalidad: 'Presencial', color: '#fde8d8', icono: 'math' },
    { id: 3, nombre: 'Física',     codigo: '4to-A', grado: '4to Secundaria - Secc. A', modalidad: 'Presencial', color: '#d9f5e0', icono: 'science' },
    { id: 4, nombre: 'Física',     codigo: '4to-B', grado: '4to Secundaria - Secc. B', modalidad: 'Presencial', color: '#d9f5e0', icono: 'science' },
  ];

  readonly cursoTabs: { id: TabCurso; label: string }[] = [
    { id: 'contenido',    label: 'Contenido' },
    { id: 'tareas',       label: 'Tareas Enviadas' },
    { id: 'notas',        label: 'Calificaciones' },
    { id: 'asistencia',   label: 'Registro Asistencia' },
  ];

  readonly bimestres = [
    {
      numero: 1, nombre: 'Bimestre 1', periodo: 'Mar – May',
      semanas: [
        { numero: 1, actividades: [] as ActividadBimestre[] },
        { numero: 2, actividades: [] as ActividadBimestre[] },
        { numero: 3, actividades: [] as ActividadBimestre[] },
      ]
    },
    { numero: 2, nombre: 'Bimestre 2', periodo: 'Jun – Jul', semanas: [{ numero: 4, actividades: [] as ActividadBimestre[] }] }
  ];

  readonly semanaActualMock = signal(1); // Para habilitar los botones en la semana 1

  // MOCK DE ALUMNOS PARA ASISTENCIA (modal)
  alumnosMock: AlumnoMock[] = [];

  // ── Tab NOTAS: estado y lógica ────────────────────────────────────────────
  filasNotas: FilaNota[] = [];
  bimestreActivo = 1;
  guardandoNotas = signal(false);
  notasGuardadasExito = signal(false);
  errorNotasSinEvidencia = signal(false);

  /** Carga (o recarga) las filas de notas para el curso y bimestre activo */
  cargarFilasNotas(): void {
    const curso = this.cursoActivo();
    if (!curso) return;
    // TODO: reemplazar con this.notaService.obtenerNotasCurso(curso.id, this.bimestreActivo)
    const alumnosMock = [
      { id: 101, nombre: 'QUISPE MAMANI, Juan' },
      { id: 102, nombre: 'GARCÍA SILVA, Ana María' },
      { id: 103, nombre: 'CONDORI LÓPEZ, Luis' },
      { id: 104, nombre: 'TORRES CRUZ, Carlos' },
      { id: 105, nombre: 'ROJAS PEÑA, Sofía' },
    ];
    this.filasNotas = this.notaService.construirFilasSimuladas(alumnosMock, curso.grado);
  }

  /** Marca la fila como modificada al cambiar la nota */
  onNotaChange(fila: FilaNota): void {
    fila.modificado = true;
  }

  /** Captura el archivo seleccionado para evidencia */
  onArchivoSeleccionado(evento: Event, fila: FilaNota): void {
    const input = evento.target as HTMLInputElement;
    if (!input.files?.length) return;
    fila.archivoPendiente = input.files[0];
    fila.conEvidencia     = true;
    fila.modificado       = true;
  }

  /** Valida y guarda todas las notas del bimestre */
  guardarNotas(): void {
    // Validar: toda nota registrada debe tener evidencia
    const sinEvidencia = this.filasNotas.some(
      f => f.modificado && !f.conEvidencia
    );
    if (sinEvidencia) {
      this.errorNotasSinEvidencia.set(true);
      setTimeout(() => this.errorNotasSinEvidencia.set(false), 4000);
      return;
    }
    this.guardandoNotas.set(true);
    this.errorNotasSinEvidencia.set(false);
    // TODO: llamar a notaService.registrarNotasLote() cuando el backend esté disponible
    // Por ahora simulamos éxito:
    setTimeout(() => {
      this.guardandoNotas.set(false);
      this.notasGuardadasExito.set(true);
      setTimeout(() => this.notasGuardadasExito.set(false), 3000);
    }, 800);
  }

  /** Returns true si una nota literal es válida */
  notaLiteralValida(fila: FilaNota): boolean {
    return fila.tipoEscala === 'LITERAL' && !!fila.notaLiteral;
  }

  /** Returns true si una nota vigesimal está en rango */
  notaVigesimalValida(fila: FilaNota): boolean {
    return fila.tipoEscala === 'VIGESIMAL' &&
      this.notaService.validarVigesimal(fila.notaVigesimal);
  }
  
  // FORMULARIO NUEVA TAREA
  nuevaTarea = {
      titulo: '',
      descripcion: '',
      fechaLimite: ''
  };

  ngOnInit(): void {
  }

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
    // Pre-cargar filas de notas para el curso seleccionado
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

  getEstadoLabel(estado: string): string {
    const mapa: Record<string, string> = {
      entregada: 'Entregada', vencida: 'Vencida',
      por_entregar: 'Por entregar', no_revisado: 'No revisado',
    };
    return mapa[estado] || estado;
  }

  // ── Modales ──────────────────────────────────────────────
  
  abrirModalAsistencia(curso: CursoCard, semana: number): void {
      this.semanaSeleccionada.set(semana);
      this.alumnosMock = [
          { id: 101, nombre: 'Quispe Mamani, Juan Perez', permisoAcademia: true, estado: 'ASISTIO' },
          { id: 102, nombre: 'García Silva, Ana María', permisoAcademia: false, estado: 'ASISTIO' },
          { id: 103, nombre: 'Condori López, Luis', permisoAcademia: false, estado: 'ASISTIO' },
          { id: 104, nombre: 'Torres Cruz, Carlos', permisoAcademia: true, estado: 'ASISTIO' },
          { id: 105, nombre: 'Rojas Peña, Sofia', permisoAcademia: false, estado: 'ASISTIO' },
      ];
      this.modalAsistenciaVisible.set(true);
  }

  abrirModalTarea(curso: CursoCard, semana: number): void {
      this.semanaSeleccionada.set(semana);
      this.nuevaTarea = { titulo: '', descripcion: '', fechaLimite: '' };
      this.modalTareaVisible.set(true);
  }

  cerrarModales(): void {
      this.modalAsistenciaVisible.set(false);
      this.modalTareaVisible.set(false);
  }

  guardarAsistencia(): void {
      console.log('Guardando asistencia...', this.alumnosMock);
      alert('Asistencia guardada con éxito.');
      this.cerrarModales();
  }

  guardarTarea(): void {
      console.log('Guardando tarea...', this.nuevaTarea);
      alert('Tarea "' + this.nuevaTarea.titulo + '" creada con éxito.');
      this.cerrarModales();
  }
}
