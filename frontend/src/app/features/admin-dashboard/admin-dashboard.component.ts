import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

// ── Tipos ──────────────────────────────────────────────────────────────────
export type TabDirector = 'panel' | 'administracion' | 'docentes' | 'calidad' | 'mensajes';

// ── Interfaces ─────────────────────────────────────────────────────────────
export interface ExpedienteAlumno {
  id: number;
  nombre: string;
  dni: string;
  grado: string;
  documentos: ('ok' | 'warn' | 'error')[];
  nee: boolean;
}

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

export interface AulaValidacion {
  nombre: string;
  estado: 'ok' | 'advertencia' | 'error';
  estudiantes: number;
  notasCompletas: number;
  notasBlanco: number;
  inconsistencias: number;
}

export interface CircularOficial {
  id: number;
  titulo: string;
  estado: 'enviado' | 'borrador';
  fecha: string;
  destinatarios: number;
}

export interface Reunion {
  id: number;
  padre: string;
  estudiante: string;
  fecha: string;
  hora: string;
  motivo: string;
  estado: 'confirmada' | 'pendiente';
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

  // ── Signals ────────────────────────────────────────────────────
  readonly tabActiva        = signal<TabDirector>('panel');
  readonly cargando         = signal(false);
  readonly busquedaExpediente = signal('');

  // ── KPIs Institucionales ───────────────────────────────────────
  readonly kpis = signal({
    alumnosTotales:   542,
    docentes:          68,
    aulas:             23,
    alertasActivas:     3,
  });

  readonly asistenciaSemanal = [
    { dia: 'Lun', alumnos: 498, docentes: 66 },
    { dia: 'Mar', alumnos: 481, docentes: 65 },
    { dia: 'Mié', alumnos: 512, docentes: 67 },
    { dia: 'Jue', alumnos: 490, docentes: 64 },
    { dia: 'Vie', alumnos: 476, docentes: 63 },
  ];

  readonly maxAlumnos = Math.max(...this.asistenciaSemanal.map(d => d.alumnos));

  // ── Alertas Centro ─────────────────────────────────────────────
  readonly alertasCentro = [
    { titulo: 'Documentos Faltantes',      subtitulo: 'DNI/Partidas sin entregar',  cantidad: 12 },
    { titulo: 'Matrículas Extemporáneas',  subtitulo: 'Pendientes de validación',   cantidad: 3  },
    { titulo: 'Evidencias Docentes',       subtitulo: 'Sin subir al sistema',        cantidad: 8  },
  ];

  // ── Administración ─────────────────────────────────────────────
  expedientes: ExpedienteAlumno[] = [];
  busquedaText = '';

  readonly expedientesFiltrados = computed(() => {
    const t = this.busquedaText.toLowerCase();
    if (!t) return this.expedientes;
    return this.expedientes.filter(e =>
      e.nombre.toLowerCase().includes(t) ||
      e.dni.includes(t) ||
      e.grado.toLowerCase().includes(t)
    );
  });

  // ── Docentes ───────────────────────────────────────────────────
  docentesSupervision: DocenteSupervision[] = [];
  auditorEvidencias: AuditorEvidencia[] = [];

  readonly kpisDocentes = {
    aprobados: 62,
    pendientes: 4,
    retrasados: 2,
  };

  // ── Calidad ────────────────────────────────────────────────────
  aulasValidacion: AulaValidacion[] = [];

  readonly kpisCalidad = {
    totalAulas: 23,
    aulasOk: 18,
    conAdvertencias: 3,
    conErrores: 2,
  };

  readonly progresValidacion = { completas: 520, total: 542 };
  readonly porcentajeValidacion = computed(() =>
    Math.round((this.progresValidacion.completas / this.progresValidacion.total) * 100)
  );

  // ── Mensajes ───────────────────────────────────────────────────
  circulares: CircularOficial[] = [];
  reuniones: Reunion[] = [];

  readonly estadisticasComunicacion = {
    circularesEnviadas: 12,
    reunionesRealizadas: 28,
    tasaRespuesta: 94,
  };

  readonly proximasReuniones = [
    { periodo: 'Hoy',         cantidad: 2 },
    { periodo: 'Esta Semana', cantidad: 4 },
  ];

  // ── Ciclo de vida ──────────────────────────────────────────────
  ngOnInit(): void {
    this.cargarDatos();
  }

  // ── Navegación ─────────────────────────────────────────────────
  setTab(tab: TabDirector): void { this.tabActiva.set(tab); }

  irAVistaDocente(): void { this.router.navigate(['/docente/asistencia']); }
  irAVistaAlumno():  void { this.router.navigate(['/alumno/portal']); }

  logout(): void { this.authService.logout(); }

  // ── Helpers ────────────────────────────────────────────────────
  alturaBarraAlumno(alumnos: number): number {
    return Math.round((alumnos / this.maxAlumnos) * 160);
  }

  alturaBarraDocente(docentes: number): number {
    return Math.round((docentes / 68) * 160);
  }

  exportarSiagie(): void {
    alert('Exportación al SIAGIE — Conectar con endpoint /api/v1/matriculas/exportar/siagie');
  }

  enviarCircular(id: number): void {
    const c = this.circulares.find(x => x.id === id);
    if (c) c.estado = 'enviado';
  }

  confirmarReunion(id: number): void {
    const r = this.reuniones.find(x => x.id === id);
    if (r) r.estado = 'confirmada';
  }

  // ── Carga de datos ─────────────────────────────────────────────
  private cargarDatos(): void {
    this.expedientes = [
      { id: 1, nombre: 'García Pérez, Ana María',      dni: '76543210', grado: '5to Primaria',    documentos: ['ok','ok','ok'],     nee: false },
      { id: 2, nombre: 'López Torres, Carlos José',    dni: '76543211', grado: '3ro Secundaria',  documentos: ['ok','warn','ok'],   nee: false },
      { id: 3, nombre: 'Ramírez Silva, María Elena',   dni: '76543212', grado: '2do Primaria',    documentos: ['error','error','ok'], nee: true  },
      { id: 4, nombre: 'Fernández Rojas, Juan Pablo',  dni: '76543213', grado: '4to Secundaria',  documentos: ['ok','ok','ok'],     nee: false },
      { id: 5, nombre: 'Sánchez Vargas, Lucía Isabel', dni: '76543214', grado: '1ro Secundaria',  documentos: ['ok','ok','error'],  nee: true  },
    ];

    this.docentesSupervision = [
      { id: 1, nombre: 'Prof. María García',   especialidad: 'Matemáticas',      estado: 'aprobado',  evidencias: 12, icono: '✓' },
      { id: 2, nombre: 'Prof. Carlos Mendoza', especialidad: 'Comunicación',     estado: 'aprobado',  evidencias: 15, icono: '✓' },
      { id: 3, nombre: 'Prof. Ana Torres',     especialidad: 'Ciencias',         estado: 'pendiente', evidencias:  8, icono: '⏰' },
      { id: 4, nombre: 'Prof. Luis Quispe',    especialidad: 'Historia',         estado: 'retrasado', evidencias:  3, icono: '✗' },
      { id: 5, nombre: 'Prof. Rosa Flores',    especialidad: 'Inglés',           estado: 'aprobado',  evidencias: 14, icono: '✓' },
      { id: 6, nombre: 'Prof. Juan Ramírez',   especialidad: 'Educación Física', estado: 'pendiente', evidencias: 10, icono: '⏰' },
      { id: 7, nombre: 'Prof. Carmen Silva',   especialidad: 'Arte',             estado: 'aprobado',  evidencias: 11, icono: '✓' },
      { id: 8, nombre: 'Prof. Diego Vargas',   especialidad: 'Matemáticas',      estado: 'pendiente', evidencias:  9, icono: '⏰' },
    ];

    this.auditorEvidencias = [
      { profesor: 'Prof. Ana Torres',   aula: '3ro A', actividad: 'Evaluación práctica', fecha: '2026-04-20', fotos: 3 },
      { profesor: 'Prof. Luis Quispe',  aula: '5to B', actividad: 'Proyecto final',       fecha: '2026-04-18', fotos: 1 },
      { profesor: 'Prof. Juan Ramírez', aula: '2do C', actividad: 'Rúbricas',             fecha: '2026-04-22', fotos: 5 },
    ];

    this.aulasValidacion = [
      { nombre: '1ro A – Primaria',   estado: 'advertencia', estudiantes: 25, notasCompletas: 23, notasBlanco: 2, inconsistencias: 0 },
      { nombre: '2do B – Primaria',   estado: 'ok',          estudiantes: 24, notasCompletas: 24, notasBlanco: 0, inconsistencias: 0 },
      { nombre: '3ro A – Secundaria', estado: 'error',       estudiantes: 28, notasCompletas: 25, notasBlanco: 3, inconsistencias: 1 },
      { nombre: '4to A – Secundaria', estado: 'ok',          estudiantes: 27, notasCompletas: 27, notasBlanco: 0, inconsistencias: 0 },
      { nombre: '5to A – Secundaria', estado: 'ok',          estudiantes: 26, notasCompletas: 26, notasBlanco: 0, inconsistencias: 0 },
    ];

    this.circulares = [
      { id: 1, titulo: 'Cronograma de Evaluaciones – II Bimestre', estado: 'enviado',  fecha: '2026-04-15', destinatarios: 542 },
      { id: 2, titulo: 'Reunión de Padres – 3ro Secundaria',       estado: 'enviado',  fecha: '2026-04-20', destinatarios:  85 },
      { id: 3, titulo: 'Actividades por Día del Trabajo',          estado: 'borrador', fecha: '2026-04-25', destinatarios: 542 },
    ];

    this.reuniones = [
      { id: 1, padre: 'Sra. María García',  estudiante: 'Ana García (5to Primaria)',   fecha: '2026-04-27', hora: '10:30 AM', motivo: 'Rendimiento académico', estado: 'confirmada' },
      { id: 2, padre: 'Sra. Rosa Torres',   estudiante: 'María Torres (2do Primaria)', fecha: '2026-04-29', hora: '09:00 AM', motivo: 'NEE – Adaptaciones',    estado: 'confirmada' },
      { id: 3, padre: 'Sr. Juan Ramírez',   estudiante: 'Luis Ramírez (4to Secundaria)', fecha: '2026-04-29', hora: '02:00 PM', motivo: 'Orientación vocacional', estado: 'pendiente' },
    ];
  }
}
