import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

interface ResumenDocente {
  id: number;
  nombre: string;
  especialidad: string;
  aula: string;
  asistenciaPromedio: number;
  alumnosConAlerta: number;
  notasRegistradas: number;
  evidenciasSubidas: number;
}

interface AlertaGlobal {
  tipo: 'FALTA_DOCENTE' | 'ALUMNOS_EN_RIESGO' | 'NOTAS_PENDIENTES';
  mensaje: string;
  aula: string;
  cantidad: number;
}

type TabDirector = 'resumen' | 'docentes' | 'alertas';

/**
 * AdminDashboardComponent – Panel de Control del Director.
 *
 * Secciones:
 *  1. Resumen General – KPIs institucionales en tiempo real
 *  2. Monitor Docentes – Estado curricular de los 68 docentes
 *  3. Alertas – Alumnos en riesgo y notas pendientes
 *
 * Standalone Component (Angular 19)
 */
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {

  private authService = inject(AuthService);

  // ── Signals ───────────────────────────────────────────────────
  readonly cargando        = signal(false);
  readonly tabActiva       = signal<TabDirector>('resumen');
  readonly anioActual      = signal(new Date().getFullYear());

  // ── KPIs Institucionales ──────────────────────────────────────
  readonly kpis = signal({
    totalAlumnos:       1500,
    alumnosActivos:     1487,
    totalDocentes:      68,
    docentesActivos:    65,
    porcentajeAsistencia: 91.4,
    alumnosEnRiesgo:    23,
    notasPendientes:    12,
    evidenciasHoy:      47,
  });

  // ── Datos ─────────────────────────────────────────────────────
  docentes: ResumenDocente[] = [];
  alertas: AlertaGlobal[] = [];
  busquedaDocente = '';

  readonly docentesFiltrados = computed(() => {
    const termino = this.busquedaDocente.toLowerCase();
    return this.docentes.filter(d =>
      d.nombre.toLowerCase().includes(termino) ||
      d.especialidad.toLowerCase().includes(termino) ||
      d.aula.toLowerCase().includes(termino)
    );
  });

  readonly tabs = [
    { id: 'resumen'  as TabDirector, label: 'Resumen General', icon: '📊' },
    { id: 'docentes' as TabDirector, label: 'Monitor Docentes', icon: '👨‍🏫' },
    { id: 'alertas'  as TabDirector, label: 'Alertas',          icon: '🚨' },
  ];

  // ── Ciclo de vida ─────────────────────────────────────────────
  ngOnInit(): void {
    this.cargarDatos();
  }

  seleccionarTab(tab: TabDirector): void {
    this.tabActiva.set(tab);
  }

  esTabActiva(tab: TabDirector): boolean {
    return this.tabActiva() === tab;
  }

  // ── Carga de datos ────────────────────────────────────────────
  private cargarDatos(): void {
    this.cargando.set(true);
    // Mock datos demo – reemplazar con llamadas reales a los servicios
    this.docentes = this.generarDocentesDemo();
    this.alertas  = this.generarAlertasDemo();
    this.cargando.set(false);
  }

  // ── Helpers ───────────────────────────────────────────────────
  getColorAsistencia(pct: number): string {
    if (pct >= 85) return 'success';
    if (pct >= 70) return 'warning';
    return 'danger';
  }

  exportarInforme(): void {
    // TODO: conectar con backend /matriculas/exportar/siagie
    alert('Exportando informe SIAGIE… Esta función conectará con el backend.');
  }

  // ── Datos Demo ────────────────────────────────────────────────
  private generarDocentesDemo(): ResumenDocente[] {
    const especialidades = ['Matemática','Comunicación','CTA','Historia','Inglés','Arte','Ed. Física','Tutoría','DPCC','Religión'];
    const grados = ['1ro','2do','3ro','4to','5to'];
    const secciones = ['A','B','C'];
    return Array.from({ length: 20 }, (_, i) => ({
      id: i + 1,
      nombre: `DOCENTE ${String(i+1).padStart(2,'0')}, Nombre Apellido`,
      especialidad: especialidades[i % especialidades.length],
      aula: `${grados[i % grados.length]} ${secciones[i % secciones.length]} – Secundaria`,
      asistenciaPromedio: 70 + Math.random() * 30,
      alumnosConAlerta: Math.floor(Math.random() * 5),
      notasRegistradas: Math.floor(Math.random() * 30) + 10,
      evidenciasSubidas: Math.floor(Math.random() * 20),
    }));
  }

  private generarAlertasDemo(): AlertaGlobal[] {
    return [
      { tipo: 'ALUMNOS_EN_RIESGO',  mensaje: 'Alumnos con más de 3 faltas consecutivas', aula: '3ro A – Secundaria', cantidad: 4 },
      { tipo: 'NOTAS_PENDIENTES',   mensaje: 'Notas del B2 pendientes de registro',       aula: '4to B – Secundaria', cantidad: 8 },
      { tipo: 'ALUMNOS_EN_RIESGO',  mensaje: 'Alumnos con asistencia menor al 75%',       aula: '5to A – Secundaria', cantidad: 3 },
      { tipo: 'NOTAS_PENDIENTES',   mensaje: 'Evidencias de examen sin subir',             aula: '2do C – Secundaria', cantidad: 12 },
      { tipo: 'ALUMNOS_EN_RIESGO',  mensaje: 'Alumnos con promedio menor a 11',           aula: '1ro A – Secundaria', cantidad: 6 },
    ];
  }
}
