import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService, DashboardKpi, AlertaDashboard } from '../../core/services/dashboard.service';
import { DocenteService, DocenteResponse } from '../../core/services/docente.service';

// ── Tipos prueba ──────────────────────────────────────────────────────────────────
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
  private dashboardService = inject(DashboardService);
  private docenteService = inject(DocenteService);

  // ── Signals ─────────────────────────────────────────────────────────────
  readonly tabActiva = signal<TabDirector>('panel');
  readonly cargando = signal(true);
  readonly busquedaExpediente = signal('');
  readonly errorConexion = signal(false);

  // ── KPIs Institucionales (desde API real) ─────────────────────────────────
  readonly kpis = signal({
    alumnosTotales: 0,
    docentes: 0,
    aulas: 0,
    alertasActivas: 0,
    presentesHoy: 0,
    faltasHoy: 0,
  });

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

  // ── Alertas Centro ─────────────────────────────────────────────
  readonly alertasCentro = [
    { titulo: 'Documentos Faltantes', subtitulo: 'DNI/Partidas sin entregar', cantidad: 12 },
    { titulo: 'Matrículas Extemporáneas', subtitulo: 'Pendientes de validación', cantidad: 3 },
    { titulo: 'Evidencias Docentes', subtitulo: 'Sin subir al sistema', cantidad: 8 },
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

  // ── Semáforo Curricular (desde API real) ────────────────────────────
  docentesSupervision: DocenteResponse[] = [];
  auditorEvidencias: AuditorEvidencia[] = [];

  readonly kpisDocentes = signal({ aprobados: 0, pendientes: 0, retrasados: 0 });

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
    { periodo: 'Hoy', cantidad: 2 },
    { periodo: 'Esta Semana', cantidad: 4 },
  ];

  // ── Ciclo de vida ────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.cargarDatos();
  }

  // ── Navegación ─────────────────────────────────────────────────
  setTab(tab: TabDirector): void { this.tabActiva.set(tab); }

  irAVistaDocente(): void { this.router.navigate(['/docente/asistencia']); }
  irAVistaAlumno(): void { this.router.navigate(['/alumno/portal']); }

  logout(): void { this.authService.logout(); }

  // ── Helpers ────────────────────────────────────────────────────────────
  alturaBarraAlumno(alumnos: number): number {
    const max = Math.max(1, ...this.asistenciaSemanal.map(d => d.alumnos));
    return Math.round((alumnos / max) * 160);
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

  // ── Carga de datos REALES desde el Backend ────────────────────────────────
  private cargarDatos(): void {
    this.cargando.set(true);
    this.errorConexion.set(false);

    // ─ 1. KPIs principales del Dashboard ────────────────────────────────────
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
  }
}

