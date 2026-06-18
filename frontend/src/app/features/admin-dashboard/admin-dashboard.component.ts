import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService, DashboardKpi, AlertaDashboard } from '../../core/services/dashboard.service';
import { DocenteService, DocenteResponse } from '../../core/services/docente.service';
import { AsistenciaService, AlertaFalta } from '../../core/services/asistencia.service';

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
  private asistenciaService = inject(AsistenciaService);

  // ── Signals ─────────────────────────────────────────────────────────────
  readonly tabActiva = signal<TabDirector>('panel');
  readonly cargando = signal(true);
  readonly busquedaExpediente = signal('');
  readonly errorConexion = signal(false);

  // ── Nombre del director desde el token JWT ────────────────────────────
  readonly directorNombre = computed(() =>
    this.authService.getUsuarioActual()?.username ?? 'Director'
  );
  readonly directorInicial = computed(() =>
    (this.authService.getUsuarioActual()?.username ?? 'D')[0]?.toUpperCase() ?? 'D'
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

  // ── Alertas: alumnos con +3 faltas consecutivas ───────────────────────
  // TODO: reemplazar con llamada real a GET /api/v1/asistencias/alertas/faltas-excesivas
  readonly alertasFaltas = signal<AlertaFalta[]>([
    { alumnoId: 1, nombreAlumno: 'QUISPE FLORES, María Fernanda',  totalFaltas: 5 },
    { alumnoId: 4, nombreAlumno: 'MENDOZA CCALLO, Carlos Alberto', totalFaltas: 4 },
    { alumnoId: 7, nombreAlumno: 'CCOICA HUAYTA, Valeria Sophia',  totalFaltas: 3 },
  ]);

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
  // TODO: reemplazar con llamada a GET /api/v1/alumnos (con paginación)
  expedientes: ExpedienteAlumno[] = [
    { id: 1,  nombre: 'QUISPE FLORES, María Fernanda',  dni: '12345678', grado: '5to A – Secundaria', documentos: ['ok', 'ok', 'warn'], nee: false },
    { id: 2,  nombre: 'PAREDES HUANCA, José Luis',      dni: '23456789', grado: '5to A – Secundaria', documentos: ['ok', 'ok', 'ok'],  nee: false },
    { id: 3,  nombre: 'TORRES RAMOS, Ana Lucía',        dni: '34567890', grado: '4to A – Secundaria', documentos: ['ok', 'warn', 'error'], nee: true },
    { id: 4,  nombre: 'MENDOZA CCALLO, Carlos Alberto', dni: '45678901', grado: '4to A – Secundaria', documentos: ['ok', 'ok', 'ok'],  nee: false },
    { id: 5,  nombre: 'GARCIA PAUCAR, Luciana Isabel',  dni: '56789012', grado: '3ro B – Secundaria', documentos: ['error', 'warn', 'ok'], nee: false },
    { id: 6,  nombre: 'VILCA MAMANI, Diego Alejandro',  dni: '67890123', grado: '3ro B – Secundaria', documentos: ['ok', 'ok', 'ok'],  nee: false },
    { id: 7,  nombre: 'CCOICA HUAYTA, Valeria Sophia',  dni: '78901234', grado: '2do A – Primaria',   documentos: ['ok', 'warn', 'ok'], nee: true },
    { id: 8,  nombre: 'LLERENA QUISPE, Rodrigo Mateo',  dni: '89012345', grado: '2do A – Primaria',   documentos: ['ok', 'ok', 'ok'],  nee: false },
  ];
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
  // TODO: reemplazar con llamada a GET /api/v1/circulares
  circulares: CircularOficial[] = [
    { id: 1, titulo: 'Cronograma de Evaluaciones – Bimestre 2', estado: 'enviado',  fecha: '2026-06-10', destinatarios: 540 },
    { id: 2, titulo: 'Jornada de Reflexión Pedagógica',          estado: 'enviado',  fecha: '2026-06-05', destinatarios: 68  },
    { id: 3, titulo: 'Actividades del Día del Logro',            estado: 'borrador', fecha: '2026-06-14', destinatarios: 0   },
    { id: 4, titulo: 'Reunión de APAFA – Julio 2026',            estado: 'borrador', fecha: '2026-06-15', destinatarios: 0   },
  ];
  // TODO: reemplazar con llamada a GET /api/v1/reuniones
  reuniones: Reunion[] = [
    { id: 1, padre: 'Sr. Jorge Quispe',   estudiante: 'QUISPE FLORES, María',   fecha: '2026-06-16', hora: '10:00', motivo: 'Bajo rendimiento – Matemática', estado: 'confirmada' },
    { id: 2, padre: 'Sra. Carmen Torres', estudiante: 'TORRES RAMOS, Ana',      fecha: '2026-06-17', hora: '11:30', motivo: 'Documentos pendientes de entrega', estado: 'pendiente' },
    { id: 3, padre: 'Sr. Luis Vilca',     estudiante: 'VILCA MAMANI, Diego',    fecha: '2026-06-18', hora: '09:00', motivo: 'Seguimiento a faltas de asistencia', estado: 'pendiente' },
  ];

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
  }
}

