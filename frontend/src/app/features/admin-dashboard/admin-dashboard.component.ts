import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService, DashboardKpi, AlertaDashboard } from '../../core/services/dashboard.service';
import { DocenteService, DocenteResponse, DocenteRequestDTO } from '../../core/services/docente.service';
import { AsistenciaService, AlertaFalta } from '../../core/services/asistencia.service';
import { AlumnoService, AlumnoRequestDTO, AlumnoResponse } from '../../core/services/alumno.service';
import { AulaService, AulaResponseDTO } from '../../core/services/aula.service';
import { CursoAsignadoService, CursoAsignadoRequestDTO } from '../../core/services/curso-asignado.service';
import { MatriculaService } from '../../core/services/matricula.service';
import { ExpedienteService, ExpedienteResumen, ExpedienteDocumento, TipoDocumentoExpediente, DOCUMENTOS_REQUERIDOS } from '../../core/services/expediente.service';
import { AREAS_CURRICULARES, AREAS_TUTOR_PRIMARIA } from '../../core/constants/areas-curriculares';
import { ReunionService, Reunion, ReunionRequestDTO } from '../../core/services/reunion.service';
import { PerfilService, Perfil } from '../../core/services/perfil.service';
import { CalidadService, AulaValidacionDTO } from '../../core/services/calidad.service';
import { CircularService, Circular, CircularRequestDTO } from '../../core/services/circular.service';
import { forkJoin } from 'rxjs';

// ── Tipos prueba ──────────────────────────────────────────────────────────────────
export type TabDirector = 'panel' | 'administracion' | 'docentes' | 'calidad' | 'mensajes';

export interface DocenteSupervision {
  id: number;
  nombre: string;
  especialidad: string;
  estado: 'aprobado' | 'pendiente' | 'retrasado';
  evidencias: number;
  icono: string;
}

export interface AuditorEvidencia {
  profesor: string;
  aula: string;
  actividad: string;
  fecha: string;
  fotos: number;
}


/**
 * AdminDashboardComponent – Panel de Control del Director.
 * 5 secciones: Panel, Administración, Docentes, Calidad, Mensajes.
 * Diseño basado en Figma "Colegio Micaela".
 */
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {

  private router = inject(Router);
  private authService = inject(AuthService);
  private dashboardService = inject(DashboardService);
  private docenteService = inject(DocenteService);
  private asistenciaService = inject(AsistenciaService);
  private alumnoService = inject(AlumnoService);
  private aulaService = inject(AulaService);
  private cursoAsignadoService = inject(CursoAsignadoService);
  private matriculaService = inject(MatriculaService);
  private expedienteService = inject(ExpedienteService);
  private reunionService = inject(ReunionService);
  private perfilService = inject(PerfilService);
  private calidadService = inject(CalidadService);
  private circularService = inject(CircularService);

  // ── Signals ─────────────────────────────────────────────────────────────
  readonly tabActiva = signal<TabDirector>('panel');
  readonly cargando = signal(true);
  readonly busquedaExpediente = signal('');
  readonly errorConexion = signal(false);

  // ── Formularios del Director (Matrícula / Docentes / Cursos) ──────────────
  readonly anioActual = new Date().getFullYear();
  readonly aulasDisponibles = signal<AulaResponseDTO[]>([]);

  readonly modalCrearAlumnoVisible = signal(false);
  readonly guardandoAlumno = signal(false);
  readonly errorAlumno = signal('');
  nuevoAlumno: AlumnoRequestDTO = this.formAlumnoVacio();

  readonly modalCrearDocenteVisible = signal(false);
  readonly guardandoDocente = signal(false);
  readonly errorDocente = signal('');
  nuevoDocente: DocenteRequestDTO = this.formDocenteVacio();

  // ── Editar Alumno ──────────────────────────────────────────────────────
  readonly modalEditarAlumnoVisible = signal(false);
  readonly guardandoEditarAlumno = signal(false);
  readonly errorEditarAlumno = signal('');
  readonly alumnoEditandoId = signal<number | null>(null);
  readonly docentesParaTutor = signal<DocenteResponse[]>([]);
  formEditarAlumno: AlumnoRequestDTO = this.formAlumnoVacio();
  estadoMatriculaEditando: 'ACTIVO' | 'RETIRADO' | 'TRASLADADO' | 'EGRESADO' = 'ACTIVO';

  // ── Editar Docente ─────────────────────────────────────────────────────
  readonly modalEditarDocenteVisible = signal(false);
  readonly guardandoEditarDocente = signal(false);
  activoEditandoDocente = true;
  readonly errorEditarDocente = signal('');
  readonly docenteEditandoId = signal<number | null>(null);
  formEditarDocente: DocenteRequestDTO = this.formDocenteVacio();

  readonly modalAsignarCursoVisible = signal(false);
  readonly guardandoCurso = signal(false);
  readonly errorCurso = signal('');
  readonly docenteParaAsignar = signal<DocenteResponse | null>(null);
  nuevaAsignacion: { aulaId: number; areaCurricular: string; comoTutor: boolean } = { aulaId: 0, areaCurricular: '', comoTutor: false };

  // ── Buscador de aula (Asignar Curso) ──────────────────────────────────────
  aulaBusqueda = '';
  readonly aulaDropdownAbierto = signal(false);
  readonly aulasFiltradas = computed(() => {
    const t = this.aulaBusqueda.trim().toLowerCase();
    const lista = this.aulasDisponibles();
    if (!t) return lista;
    return lista.filter(a => a.descripcion.toLowerCase().includes(t));
  });

  // ── Catálogo fijo de áreas curriculares (Asignar Curso) ───────────────────
  readonly areasCurriculares = AREAS_CURRICULARES;
  readonly aulaSeleccionadaAsignacion = computed(() =>
    this.aulasDisponibles().find(a => a.id === this.nuevaAsignacion.aulaId) ?? null
  );
  readonly esAulaPrimaria = computed(() => this.aulaSeleccionadaAsignacion()?.nivel === 'PRIMARIA');

  /**
   * Plazo institucional habitual para asignar cursos: 15 de marzo del año académico.
   * Es solo un indicador informativo — no bloquea la asignación, ya que a mitad de año
   * puede ser necesario reemplazar a un docente por motivos externos.
   */
  readonly fueraDePlazoAsignacion = new Date() > new Date(this.anioActual, 2, 15, 23, 59, 59);

  readonly modalCredencialesVisible = signal(false);
  readonly credencialesGeneradas = signal<{ tipo: string; username: string; password: string } | null>(null);

  // ── Mi Perfil (editable por el propio usuario) ─────────────────────────
  readonly perfil = signal<Perfil | null>(null);
  readonly modalPerfilVisible = signal(false);
  readonly guardandoPerfil = signal(false);
  readonly errorPerfil = signal('');
  formPerfil: { nombreCompleto: string; celular: string; email: string } = { nombreCompleto: '', celular: '', email: '' };

  readonly directorNombre = computed(() =>
    this.perfil()?.nombreCompleto || this.authService.getUsuarioActual()?.username || 'Director'
  );
  readonly directorInicial = computed(() =>
    this.directorNombre()[0]?.toUpperCase() ?? 'D'
  );

  // ── KPIs Institucionales (desde API real) ─────────────────────────────────
  readonly kpis = signal({
    alumnosTotales: 0,
    docentes: 0,
    aulas: 0,
    alertasActivas: 0,
    presentesHoy: 0,
    faltasHoy: 0,
  });

  // ── Alertas: alumnos con +3 faltas consecutivas (todas las aulas) ─────────
  readonly alertasFaltas = signal<AlertaFalta[]>([]);

  readonly asistenciaSemanal: { dia: string; alumnos: number; docentes: number }[] = [
    { dia: 'Lun', alumnos: 0, docentes: 0 },
    { dia: 'Mar', alumnos: 0, docentes: 0 },
    { dia: 'Mié', alumnos: 0, docentes: 0 },
    { dia: 'Jue', alumnos: 0, docentes: 0 },
    { dia: 'Vie', alumnos: 0, docentes: 0 },
  ];

  readonly maxAlumnos = computed(() =>
    Math.max(1, ...this.asistenciaSemanal.map(d => d.alumnos))
  );

  // ── Alertas Centro (desde GET /dashboard/alertas) ────────────────
  readonly alertasCentro = signal<AlertaDashboard[]>([]);

  // ── Administración: Expediente Digital ───────────────────────────────────
  readonly expedientes = signal<ExpedienteResumen[]>([]);
  busquedaText = '';

  readonly expedientesFiltrados = computed(() => {
    const t = this.busquedaText.toLowerCase();
    const lista = this.expedientes();
    if (!t) return lista;
    return lista.filter(e =>
      e.nombreCompleto.toLowerCase().includes(t) ||
      e.dni.includes(t) ||
      (e.aulaDescripcion ?? '').toLowerCase().includes(t)
    );
  });

  readonly documentosRequeridos = DOCUMENTOS_REQUERIDOS;
  readonly modalExpedienteVisible = signal(false);
  readonly alumnoExpediente = signal<ExpedienteResumen | null>(null);
  readonly documentosAlumno = signal<ExpedienteDocumento[]>([]);
  readonly cargandoDocumentos = signal(false);
  readonly subiendoTipo = signal<TipoDocumentoExpediente | null>(null);
  readonly errorExpediente = signal('');

  // ── Semáforo Curricular (desde API real) ────────────────────────────
  docentesSupervision: DocenteResponse[] = [];
  auditorEvidencias: AuditorEvidencia[] = [];

  readonly kpisDocentes = signal({ aprobados: 0, pendientes: 0, retrasados: 0 });

  // ── Calidad (Validador de Pre-Actas, desde API real) ────────────────────
  readonly aulasValidacion = signal<AulaValidacionDTO[]>([]);
  readonly kpisCalidad = signal({ totalAulas: 0, aulasOk: 0, conAdvertencias: 0, conErrores: 0 });
  readonly progresValidacion = signal({ completas: 0, total: 0 });
  readonly porcentajeValidacion = computed(() => {
    const p = this.progresValidacion();
    return p.total > 0 ? Math.round((p.completas / p.total) * 100) : 0;
  });
  readonly registrosPendientesValidacion = computed(() =>
    this.aulasValidacion().reduce((acc, a) => acc + a.notasBlanco + a.inconsistencias, 0)
  );

  // ── Mensajes: Circulares (comunicados internos, desde API real) ──────────
  readonly circulares = signal<Circular[]>([]);
  readonly modalCrearCircularVisible = signal(false);
  readonly guardandoCircular = signal(false);
  readonly errorCircular = signal('');
  nuevaCircular: CircularRequestDTO = { titulo: '', contenido: '', dirigidoA: 'TODOS' };

  readonly estadisticasComunicacion = computed(() => ({
    circularesEnviadas: this.circulares().filter(c => c.publicada).length,
    reunionesRealizadas: this.reunionesProximas().filter(r => r.estado === 'REALIZADA').length,
    tasaRespuesta: 94, // TODO: sin concepto de "respuesta del apoderado" aún — placeholder
  }));

  // ── Reuniones (agenda real con apoderados) ─────────────────────────────
  readonly reunionesProximas = signal<Reunion[]>([]);

  readonly modalReunionVisible = signal(false);
  readonly guardandoReunion = signal(false);
  readonly errorReunion = signal('');
  nuevaReunion: { tipo: 'individual' | 'general'; alumnoId: number; aulaId: number; fecha: string; horaInicio: string; horaFin: string; motivo: string }
    = this.formReunionVacio();

  // Buscador de alumno para reunión individual
  alumnoReunionBusqueda = '';
  readonly alumnoReunionEncontrado = signal<AlumnoResponse | null>(null);
  readonly buscandoAlumnoReunion = signal(false);

  // ── Ciclo de vida ────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.cargarDatos();
  }

  // ── Navegación ─────────────────────────────────────────────────
  setTab(tab: TabDirector): void { this.tabActiva.set(tab); }

  irAVistaDocente(): void { this.router.navigate(['/docente/portal']); }
  irAVistaAlumno(): void { this.router.navigate(['/alumno/portal']); }

  logout(): void { this.authService.logout(); }

  // ── Matricular Alumno ──────────────────────────────────────────────────
  abrirModalCrearAlumno(): void {
    this.nuevoAlumno = this.formAlumnoVacio();
    this.errorAlumno.set('');
    this.modalCrearAlumnoVisible.set(true);
  }

  cerrarModalCrearAlumno(): void {
    this.modalCrearAlumnoVisible.set(false);
  }

  guardarAlumno(): void {
    this.guardandoAlumno.set(true);
    this.errorAlumno.set('');
    this.alumnoService.crear(this.nuevoAlumno).subscribe({
      next: (res) => {
        this.guardandoAlumno.set(false);
        this.modalCrearAlumnoVisible.set(false);
        this.credencialesGeneradas.set({
          tipo: 'Alumno',
          username: res.usernameGenerado,
          password: res.passwordInicial
        });
        this.modalCredencialesVisible.set(true);
      },
      error: (err) => {
        this.guardandoAlumno.set(false);
        this.errorAlumno.set(err?.error?.error || 'Error al registrar el alumno.');
      }
    });
  }

  // ── Crear Docente ────────────────────────────────────────────────────────
  abrirModalCrearDocente(): void {
    this.nuevoDocente = this.formDocenteVacio();
    this.errorDocente.set('');
    this.modalCrearDocenteVisible.set(true);
  }

  cerrarModalCrearDocente(): void {
    this.modalCrearDocenteVisible.set(false);
  }

  guardarDocente(): void {
    this.guardandoDocente.set(true);
    this.errorDocente.set('');
    this.docenteService.crear(this.nuevoDocente).subscribe({
      next: (res) => {
        this.guardandoDocente.set(false);
        this.modalCrearDocenteVisible.set(false);
        this.docenteService.obtenerSemaforo().subscribe({
          next: (docentes) => { this.docentesSupervision = docentes; },
          error: () => {}
        });
        this.credencialesGeneradas.set({
          tipo: 'Docente',
          username: res.usernameGenerado,
          password: res.passwordInicial
        });
        this.modalCredencialesVisible.set(true);
      },
      error: (err) => {
        this.guardandoDocente.set(false);
        this.errorDocente.set(err?.error?.error || 'Error al registrar el docente.');
      }
    });
  }

  // ── Editar Alumno ──────────────────────────────────────────────────────
  abrirModalEditarAlumno(alumnoId: number): void {
    this.errorEditarAlumno.set('');
    this.alumnoEditandoId.set(alumnoId);
    this.modalEditarAlumnoVisible.set(true);

    if (!this.docentesParaTutor().length) {
      this.docenteService.listarTodos().subscribe({
        next: (docentes) => this.docentesParaTutor.set(docentes),
        error: () => {}
      });
    }

    this.alumnoService.buscarPorId(alumnoId).subscribe({
      next: (a) => {
        this.formEditarAlumno = {
          dni: a.dni,
          apellidoPaterno: a.apellidoPaterno,
          apellidoMaterno: a.apellidoMaterno,
          nombres: a.nombres,
          anioAcademico: a.anioAcademico || this.anioActual,
          tienePermisoAcademia: a.tienePermisoAcademia,
          horaEntradaAcademia: a.horaEntradaAcademia,
          nombreApoderado: a.nombreApoderado,
          celularApoderado: a.celularApoderado,
          emailApoderado: a.emailApoderado,
          tutorId: a.tutorId
        };
        this.estadoMatriculaEditando = a.estadoMatricula || 'ACTIVO';
      },
      error: () => {
        this.errorEditarAlumno.set('No se pudo cargar el alumno.');
      }
    });
  }

  cerrarModalEditarAlumno(): void {
    this.modalEditarAlumnoVisible.set(false);
  }

  guardarEditarAlumno(): void {
    const id = this.alumnoEditandoId();
    if (!id) return;

    this.guardandoEditarAlumno.set(true);
    this.errorEditarAlumno.set('');
    forkJoin([
      this.alumnoService.actualizar(id, this.formEditarAlumno),
      this.alumnoService.actualizarEstado(id, this.estadoMatriculaEditando)
    ]).subscribe({
      next: () => {
        this.guardandoEditarAlumno.set(false);
        this.modalEditarAlumnoVisible.set(false);
        this.expedienteService.listarResumen(this.anioActual).subscribe({
          next: (resumen) => this.expedientes.set(resumen),
          error: () => {}
        });
      },
      error: (err) => {
        this.guardandoEditarAlumno.set(false);
        this.errorEditarAlumno.set(err?.error?.error || 'Error al guardar los cambios.');
      }
    });
  }

  // ── Editar Docente ─────────────────────────────────────────────────────
  abrirModalEditarDocente(docente: DocenteResponse): void {
    this.errorEditarDocente.set('');
    this.docenteEditandoId.set(docente.id);
    this.formEditarDocente = {
      dni: docente.dni,
      apellidoPaterno: docente.apellidoPaterno,
      apellidoMaterno: docente.apellidoMaterno,
      nombres: docente.nombres,
      especialidad: docente.especialidad,
      condicion: docente.condicion,
      emailInstitucional: docente.emailInstitucional,
      celular: docente.celular
    };
    this.activoEditandoDocente = docente.activo;
    this.modalEditarDocenteVisible.set(true);
  }

  cerrarModalEditarDocente(): void {
    this.modalEditarDocenteVisible.set(false);
  }

  guardarEditarDocente(): void {
    const id = this.docenteEditandoId();
    if (!id) return;

    this.guardandoEditarDocente.set(true);
    this.errorEditarDocente.set('');
    forkJoin([
      this.docenteService.actualizar(id, this.formEditarDocente),
      this.docenteService.actualizarEstado(id, this.activoEditandoDocente)
    ]).subscribe({
      next: () => {
        this.guardandoEditarDocente.set(false);
        this.modalEditarDocenteVisible.set(false);
        this.docenteService.obtenerSemaforo().subscribe({
          next: (docentes) => { this.docentesSupervision = docentes; },
          error: () => {}
        });
      },
      error: (err) => {
        this.guardandoEditarDocente.set(false);
        this.errorEditarDocente.set(err?.error?.error || 'Error al guardar los cambios.');
      }
    });
  }

  // ── Asignar Curso a un Docente ───────────────────────────────────────────
  abrirModalAsignarCurso(docente: DocenteResponse): void {
    this.docenteParaAsignar.set(docente);
    this.nuevaAsignacion = { aulaId: 0, areaCurricular: '', comoTutor: false };
    this.aulaBusqueda = '';
    this.aulaDropdownAbierto.set(false);
    this.errorCurso.set('');
    if (!this.aulasDisponibles().length) {
      this.cargarAulas();
    }
    this.modalAsignarCursoVisible.set(true);
  }

  cerrarModalAsignarCurso(): void {
    this.modalAsignarCursoVisible.set(false);
  }

  seleccionarAula(aula: AulaResponseDTO): void {
    this.nuevaAsignacion.aulaId = aula.id;
    this.aulaBusqueda = aula.descripcion;
    this.aulaDropdownAbierto.set(false);
    // La opción "Tutor de Aula" solo aplica a PRIMARIA; al cambiar de aula se reinicia.
    this.nuevaAsignacion.comoTutor = false;
    this.nuevaAsignacion.areaCurricular = '';
  }

  /** Elimina un aula creada por error (sin alumnos activos ni cursos asignados). */
  eliminarAula(aula: AulaResponseDTO, event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();
    if (!confirm(`¿Eliminar el aula "${aula.descripcion}"? Solo es posible si está vacía.`)) return;

    this.aulaService.eliminar(aula.id).subscribe({
      next: () => {
        this.aulasDisponibles.update(lista => lista.filter(a => a.id !== aula.id));
        if (this.nuevaAsignacion.aulaId === aula.id) {
          this.nuevaAsignacion.aulaId = 0;
          this.aulaBusqueda = '';
        }
      },
      error: (err) => alert(err?.error?.error || 'No se pudo eliminar el aula.')
    });
  }

  guardarAsignacionCurso(): void {
    const docente = this.docenteParaAsignar();
    if (!docente) return;
    if (!this.nuevaAsignacion.aulaId) {
      this.errorCurso.set('Seleccione un aula de la lista.');
      return;
    }

    this.guardandoCurso.set(true);
    this.errorCurso.set('');

    // Docente-tutor de PRIMARIA: una sola acción asigna todas las áreas
    // que no requieren especialista (Matemática, Comunicación, etc.).
    const areas = this.nuevaAsignacion.comoTutor
      ? AREAS_TUTOR_PRIMARIA
      : [this.nuevaAsignacion.areaCurricular];

    if (!this.nuevaAsignacion.comoTutor && !this.nuevaAsignacion.areaCurricular) {
      this.guardandoCurso.set(false);
      this.errorCurso.set('Seleccione un área curricular.');
      return;
    }

    const solicitudes = areas.map(area => {
      const dto: CursoAsignadoRequestDTO = {
        docenteId: docente.id,
        aulaId: this.nuevaAsignacion.aulaId,
        areaCurricular: area,
        anioAcademico: this.anioActual
      };
      return this.cursoAsignadoService.crear(dto);
    });

    forkJoin(solicitudes).subscribe({
      next: () => {
        this.guardandoCurso.set(false);
        this.modalAsignarCursoVisible.set(false);
        this.docenteService.obtenerSemaforo().subscribe({
          next: (docentes) => { this.docentesSupervision = docentes; },
          error: () => {}
        });
      },
      error: (err) => {
        this.guardandoCurso.set(false);
        this.errorCurso.set(err?.error?.error || 'Error al asignar el curso.');
      }
    });
  }

  cerrarModalCredenciales(): void {
    this.modalCredencialesVisible.set(false);
    this.credencialesGeneradas.set(null);
  }

  irAMatriculas(): void {
    this.cerrarModalCredenciales();
    this.router.navigate(['/matriculas']);
  }

  private cargarAulas(): void {
    this.aulaService.listarPorAnio(this.anioActual).subscribe({
      next: (aulas) => this.aulasDisponibles.set(aulas),
      error: (err) => console.error('Error al cargar aulas', err)
    });
  }

  private formAlumnoVacio(): AlumnoRequestDTO {
    return {
      dni: '',
      apellidoPaterno: '',
      apellidoMaterno: '',
      nombres: '',
      anioAcademico: this.anioActual,
      tienePermisoAcademia: false
    };
  }

  private formDocenteVacio(): DocenteRequestDTO {
    return {
      dni: '',
      apellidoPaterno: '',
      apellidoMaterno: '',
      nombres: '',
      condicion: 'NOMBRADO'
    };
  }

  private formReunionVacio(): { tipo: 'individual' | 'general'; alumnoId: number; aulaId: number; fecha: string; horaInicio: string; horaFin: string; motivo: string } {
    return { tipo: 'individual', alumnoId: 0, aulaId: 0, fecha: '', horaInicio: '', horaFin: '', motivo: '' };
  }

  // ── Mi Perfil ──────────────────────────────────────────────────────────
  abrirModalPerfil(): void {
    this.errorPerfil.set('');
    this.modalPerfilVisible.set(true);
    const actual = this.perfil();
    if (actual) {
      this.formPerfil = { nombreCompleto: actual.nombreCompleto || '', celular: actual.celular || '', email: actual.email || '' };
    }
    this.perfilService.obtener().subscribe({
      next: (p) => {
        this.perfil.set(p);
        this.formPerfil = { nombreCompleto: p.nombreCompleto || '', celular: p.celular || '', email: p.email || '' };
      },
      error: (err) => console.error('Error al cargar el perfil', err)
    });
  }

  cerrarModalPerfil(): void {
    this.modalPerfilVisible.set(false);
  }

  guardarPerfil(): void {
    this.guardandoPerfil.set(true);
    this.errorPerfil.set('');
    this.perfilService.actualizar(this.formPerfil).subscribe({
      next: (p) => {
        this.guardandoPerfil.set(false);
        this.perfil.set(p);
        this.modalPerfilVisible.set(false);
      },
      error: (err) => {
        this.guardandoPerfil.set(false);
        this.errorPerfil.set(err?.error?.error || 'Error al guardar el perfil.');
      }
    });
  }

  // ── Reuniones con Apoderados (link WhatsApp) ──────────────────────────────
  abrirModalReunion(): void {
    this.nuevaReunion = this.formReunionVacio();
    this.alumnoReunionBusqueda = '';
    this.alumnoReunionEncontrado.set(null);
    this.errorReunion.set('');
    if (!this.aulasDisponibles().length) {
      this.cargarAulas();
    }
    this.modalReunionVisible.set(true);
  }

  cerrarModalReunion(): void {
    this.modalReunionVisible.set(false);
  }

  buscarAlumnoReunion(): void {
    const dni = this.alumnoReunionBusqueda.trim();
    if (dni.length !== 8) {
      this.errorReunion.set('Ingrese un DNI válido de 8 dígitos.');
      return;
    }
    this.errorReunion.set('');
    this.buscandoAlumnoReunion.set(true);
    this.alumnoService.buscarPorDni(dni).subscribe({
      next: (alumno) => {
        this.buscandoAlumnoReunion.set(false);
        this.alumnoReunionEncontrado.set(alumno);
        this.nuevaReunion.alumnoId = alumno.id;
      },
      error: () => {
        this.buscandoAlumnoReunion.set(false);
        this.errorReunion.set('No se encontró ningún alumno con ese DNI.');
      }
    });
  }

  guardarReunion(): void {
    if (this.nuevaReunion.tipo === 'individual' && !this.nuevaReunion.alumnoId) {
      this.errorReunion.set('Busque y seleccione un alumno.');
      return;
    }
    if (this.nuevaReunion.tipo === 'general' && !this.nuevaReunion.aulaId) {
      this.errorReunion.set('Seleccione un aula.');
      return;
    }
    if (!this.nuevaReunion.fecha || !this.nuevaReunion.horaInicio || !this.nuevaReunion.horaFin || !this.nuevaReunion.motivo) {
      this.errorReunion.set('Complete fecha, horario y motivo.');
      return;
    }

    const dto: ReunionRequestDTO = {
      fecha: this.nuevaReunion.fecha,
      horaInicio: this.nuevaReunion.horaInicio,
      horaFin: this.nuevaReunion.horaFin,
      motivo: this.nuevaReunion.motivo
    };

    this.guardandoReunion.set(true);
    this.errorReunion.set('');

    const manejarExito = () => {
      this.guardandoReunion.set(false);
      this.modalReunionVisible.set(false);
      this.cargarReunionesProximas();
    };
    const manejarError = (err: any) => {
      this.guardandoReunion.set(false);
      this.errorReunion.set(err?.error?.error || 'Error al agendar la reunión.');
    };

    if (this.nuevaReunion.tipo === 'individual') {
      this.reunionService.crear({ ...dto, alumnoId: this.nuevaReunion.alumnoId })
        .subscribe({ next: manejarExito, error: manejarError });
    } else {
      this.reunionService.crearParaAula(this.nuevaReunion.aulaId, dto)
        .subscribe({ next: manejarExito, error: manejarError });
    }
  }

  private cargarReunionesProximas(): void {
    this.reunionService.listarProximas().subscribe({
      next: (reuniones) => this.reunionesProximas.set(reuniones),
      error: (err) => console.error('Error al cargar reuniones', err)
    });
  }

  linkWhatsApp(reunion: Reunion): string | null {
    return ReunionService.generarLinkWhatsApp(reunion);
  }

  // ── Helpers ────────────────────────────────────────────────────────────
  alturaBarraAlumno(alumnos: number): number {
    const max = Math.max(1, ...this.asistenciaSemanal.map(d => d.alumnos));
    return Math.round((alumnos / max) * 160);
  }

  alturaBarraDocente(docentes: number): number {
    return Math.round((docentes / 68) * 160);
  }

  private descargarBlob(blob: Blob, nombreArchivo: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombreArchivo;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  exportarDocentes(): void {
    this.docenteService.exportar().subscribe({
      next: (blob) => this.descargarBlob(blob, 'docentes.xlsx'),
      error: (err) => {
        console.error('Error al exportar docentes', err);
        alert('No se pudo generar el archivo. Intente nuevamente.');
      }
    });
  }

  exportarResumenDashboard(): void {
    this.dashboardService.exportar(this.anioActual).subscribe({
      next: (blob) => this.descargarBlob(blob, `dashboard_resumen_${this.anioActual}.xlsx`),
      error: (err) => {
        console.error('Error al exportar resumen', err);
        alert('No se pudo generar el archivo. Intente nuevamente.');
      }
    });
  }

  exportarSiagie(): void {
    this.matriculaService.exportarSiagie(this.anioActual).subscribe({
      next: (blob) => this.descargarBlob(blob, `matricula_siagie_${this.anioActual}.xlsx`),
      error: (err) => {
        console.error('Error al exportar SIAGIE', err);
        alert('No se pudo generar el archivo. Intente nuevamente.');
      }
    });
  }

  abrirModalCrearCircular(): void {
    this.nuevaCircular = { titulo: '', contenido: '', dirigidoA: 'TODOS' };
    this.errorCircular.set('');
    this.modalCrearCircularVisible.set(true);
  }

  cerrarModalCrearCircular(): void {
    this.modalCrearCircularVisible.set(false);
  }

  guardarCircular(): void {
    this.guardandoCircular.set(true);
    this.errorCircular.set('');
    this.circularService.crear(this.nuevaCircular).subscribe({
      next: () => {
        this.guardandoCircular.set(false);
        this.modalCrearCircularVisible.set(false);
        this.cargarCirculares();
      },
      error: (err) => {
        this.guardandoCircular.set(false);
        this.errorCircular.set(err?.error?.error || 'Error al crear la circular.');
      }
    });
  }

  enviarCircular(id: number): void {
    this.circularService.publicar(id).subscribe({
      next: () => this.cargarCirculares(),
      error: (err) => console.error('Error al publicar la circular:', err)
    });
  }

  private cargarCirculares(): void {
    this.circularService.listar().subscribe({
      next: (circulares) => this.circulares.set(circulares),
      error: (err) => console.error('Error al cargar circulares:', err)
    });
  }

  // ── Carga de datos REALES desde el Backend ────────────────────────────────
  private cargarDatos(): void {
    this.cargando.set(true);
    this.errorConexion.set(false);

    // ─ 1. KPIs principales del Dashboard ────────────────────────────────────
    // NOTA: Los datos simulados (expedientes, circulares, reuniones, alertasFaltas)
    // ya están declarados arriba como arrays constantes.
    // Se reemplazarán por llamadas HTTP cuando el backend esté disponible.
    this.dashboardService.obtenerKpis().subscribe({
      next: (kpi) => {
        this.kpis.set({
          alumnosTotales: kpi.alumnosTotales,
          docentes:       kpi.docentesTotales,
          aulas:          kpi.aulasTotales,
          alertasActivas: kpi.alumnosMatriculaProvisional,
          presentesHoy:   kpi.alumnosPresentesHoy,
          faltasHoy:      kpi.alumnosFaltasHoy,
        });
        // Actualizar gráfico de barras con datos reales de la semana
        if (kpi.asistenciaSemanal?.length) {
          kpi.asistenciaSemanal.forEach((d, i) => {
            if (this.asistenciaSemanal[i]) {
              this.asistenciaSemanal[i].alumnos  = d.alumnos;
              this.asistenciaSemanal[i].docentes = d.docentes;
            }
          });
        }
        this.kpisDocentes.set({
          aprobados:  kpi.docentesAprobados,
          pendientes: kpi.docentesPendientes,
          retrasados: kpi.docentesRetrasados,
        });
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('Error al cargar KPIs del dashboard:', err);
        this.errorConexion.set(true);
        this.cargando.set(false);
      }
    });

    // ─ 2. Semáforo Curricular (lista de docentes) ────────────────────────────
    this.docenteService.obtenerSemaforo().subscribe({
      next: (docentes) => { this.docentesSupervision = docentes; },
      error: (err) => console.error('Error al cargar semáforo:', err)
    });

    // ─ 3. Alumnos con faltas excesivas (todas las aulas) ─────────────────────
    const hoy = new Date();
    const inicioAnio = `${hoy.getFullYear()}-01-01`;
    const finHoy = hoy.toISOString().split('T')[0];
    this.asistenciaService.obtenerAlertasFaltasExcesivasGlobal(inicioAnio, finHoy).subscribe({
      next: (alertas) => this.alertasFaltas.set(alertas),
      error: (err) => console.error('Error al cargar alertas de faltas:', err)
    });

    // ─ 4. Centro de Alertas (documentos faltantes / matrículas provisionales) ─
    this.dashboardService.obtenerAlertas().subscribe({
      next: (alertas) => this.alertasCentro.set(alertas),
      error: (err) => console.error('Error al cargar alertas del centro:', err)
    });

    // ─ 5. Expediente Digital (resumen por alumno, para la pestaña Administración) ─
    this.expedienteService.listarResumen(this.anioActual).subscribe({
      next: (resumen) => this.expedientes.set(resumen),
      error: (err) => console.error('Error al cargar expedientes:', err)
    });

    // ─ 6. Mi Perfil (nombre real para la barra lateral) ──────────────────────
    this.perfilService.obtener().subscribe({
      next: (p) => this.perfil.set(p),
      error: (err) => console.error('Error al cargar el perfil:', err)
    });

    // ─ 7. Reuniones próximas con apoderados ───────────────────────────────
    this.cargarReunionesProximas();

    // ─ 8. Calidad: Validador de Pre-Actas ─────────────────────────────────
    this.calidadService.listarValidacionAulas(this.anioActual).subscribe({
      next: (aulas) => {
        this.aulasValidacion.set(aulas);
        const estudiantesTotal = aulas.reduce((acc, a) => acc + a.estudiantes, 0);
        const estudiantesConNotas = aulas.reduce((acc, a) => acc + a.notasCompletas, 0);
        this.progresValidacion.set({ completas: estudiantesConNotas, total: estudiantesTotal });
        this.kpisCalidad.set({
          totalAulas: aulas.length,
          aulasOk: aulas.filter(a => a.estado === 'ok').length,
          conAdvertencias: aulas.filter(a => a.estado === 'advertencia').length,
          conErrores: aulas.filter(a => a.estado === 'error').length,
        });
      },
      error: (err) => console.error('Error al cargar validación de calidad:', err)
    });

    // ─ 9. Circulares (comunicados internos) ───────────────────────────────
    this.cargarCirculares();
  }

  // ── Expediente Digital: ver / subir / borrar documentos ───────────────────
  abrirModalExpediente(exp: ExpedienteResumen): void {
    this.alumnoExpediente.set(exp);
    this.documentosAlumno.set([]);
    this.errorExpediente.set('');
    this.modalExpedienteVisible.set(true);
    this.cargarDocumentosAlumno(exp.alumnoId);
  }

  cerrarModalExpediente(): void {
    this.modalExpedienteVisible.set(false);
    this.alumnoExpediente.set(null);
  }

  private cargarDocumentosAlumno(alumnoId: number): void {
    this.cargandoDocumentos.set(true);
    this.expedienteService.listarDocumentos(alumnoId).subscribe({
      next: (docs) => {
        this.documentosAlumno.set(docs);
        this.cargandoDocumentos.set(false);
      },
      error: (err) => {
        console.error('Error al cargar documentos del expediente:', err);
        this.errorExpediente.set('No se pudieron cargar los documentos.');
        this.cargandoDocumentos.set(false);
      }
    });
  }

  documentoDe(tipo: TipoDocumentoExpediente): ExpedienteDocumento | undefined {
    return this.documentosAlumno().find(d => d.tipoDocumento === tipo);
  }

  subirDocumento(tipo: TipoDocumentoExpediente, input: HTMLInputElement): void {
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo) return;

    if (archivo.type !== 'application/pdf') {
      this.errorExpediente.set('Solo se admiten archivos en formato PDF.');
      return;
    }

    const alumno = this.alumnoExpediente();
    if (!alumno) return;

    this.errorExpediente.set('');
    this.subiendoTipo.set(tipo);
    this.expedienteService.subirDocumento(alumno.alumnoId, archivo, tipo).subscribe({
      next: () => {
        this.subiendoTipo.set(null);
        this.cargarDocumentosAlumno(alumno.alumnoId);
        this.refrescarResumenExpedientes();
      },
      error: (err) => {
        this.subiendoTipo.set(null);
        this.errorExpediente.set(err?.error?.error || 'Error al subir el documento.');
      }
    });
  }

  eliminarDocumento(doc: ExpedienteDocumento): void {
    if (!confirm(`¿Eliminar "${doc.nombreArchivo}"?`)) return;
    const alumno = this.alumnoExpediente();
    if (!alumno) return;

    this.expedienteService.eliminarDocumento(doc.id).subscribe({
      next: () => {
        this.cargarDocumentosAlumno(alumno.alumnoId);
        this.refrescarResumenExpedientes();
      },
      error: (err) => this.errorExpediente.set(err?.error?.error || 'Error al eliminar el documento.')
    });
  }

  private refrescarResumenExpedientes(): void {
    this.expedienteService.listarResumen(this.anioActual).subscribe({
      next: (resumen) => this.expedientes.set(resumen),
      error: () => {}
    });
  }
}

