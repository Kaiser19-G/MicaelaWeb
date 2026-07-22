import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { AulaService, AulaResponseDTO, AulaResumenAsistencia } from '../../core/services/aula.service';
import { CursoAsignadoService } from '../../core/services/curso-asignado.service';
import { CursoAsignado } from '../../core/services/docente.service';
import { AlumnoService, AlumnoResponse } from '../../core/services/alumno.service';
import { generarLinkWhatsApp } from '../../core/utils/whatsapp.util';

type Periodo = 'DIA' | 'SEMANA' | 'MES' | 'ANIO';

interface EdicionHorario {
  diaSemana: string;
  horaInicio: string;
  horaFin: string;
}

const DIAS_SEMANA = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'];
const DIA_LABEL: Record<string, string> = {
  LUNES: 'Lunes', MARTES: 'Martes', MIERCOLES: 'Miércoles', JUEVES: 'Jueves', VIERNES: 'Viernes'
};

@Component({
  selector: 'app-aula-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './aula-dashboard.component.html',
  styleUrls: ['./aula-dashboard.component.scss']
})
export class AulaDashboardComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private aulaService = inject(AulaService);
  private cursoAsignadoService = inject(CursoAsignadoService);
  private alumnoService = inject(AlumnoService);

  readonly diasSemana = DIAS_SEMANA;
  readonly diaLabel = DIA_LABEL;

  aulaId = 0;
  readonly aula = signal<AulaResponseDTO | null>(null);
  readonly horario = signal<CursoAsignado[]>([]);
  readonly alumnos = signal<AlumnoResponse[]>([]);
  readonly resumen = signal<AulaResumenAsistencia | null>(null);
  readonly cargando = signal(true);
  readonly errorMsg = signal('');

  readonly periodo = signal<Periodo>('SEMANA');
  mes = new Date().getMonth() + 1;
  anio = new Date().getFullYear();
  readonly mesesDelAnio = [
    { valor: 1, nombre: 'Enero' }, { valor: 2, nombre: 'Febrero' }, { valor: 3, nombre: 'Marzo' },
    { valor: 4, nombre: 'Abril' }, { valor: 5, nombre: 'Mayo' }, { valor: 6, nombre: 'Junio' },
    { valor: 7, nombre: 'Julio' }, { valor: 8, nombre: 'Agosto' }, { valor: 9, nombre: 'Septiembre' },
    { valor: 10, nombre: 'Octubre' }, { valor: 11, nombre: 'Noviembre' }, { valor: 12, nombre: 'Diciembre' },
  ];

  readonly horarioConHora = signal<CursoAsignado[]>([]);
  readonly horarioSinHora = signal<CursoAsignado[]>([]);
  private edicionesHorario: Record<number, EdicionHorario> = {};
  readonly guardandoHorarioId = signal<number | null>(null);
  readonly errorHorario = signal('');

  mensajeApoderados = '';

  ngOnInit(): void {
    this.aulaId = Number(this.route.snapshot.paramMap.get('id'));
    this.cargarTodo();
  }

  private cargarTodo(): void {
    this.cargando.set(true);
    this.errorMsg.set('');
    forkJoin({
      aula: this.aulaService.buscarPorId(this.aulaId),
      horario: this.cursoAsignadoService.listarPorAula(this.aulaId),
      alumnos: this.alumnoService.listarPorAula(this.aulaId)
    }).subscribe({
      next: ({ aula, horario, alumnos }) => {
        this.aula.set(aula);
        this.mensajeApoderados =
          `Estimado(a) apoderado(a), le escribimos de la I.E. Micaela Bastidas sobre el aula ${aula.descripcion}.`;
        this.actualizarHorario(horario);
        this.alumnos.set(alumnos);
        this.cargando.set(false);
        this.cargarResumen();
      },
      error: (err) => {
        console.error('Error al cargar el dashboard del aula:', err);
        this.errorMsg.set('No se pudo cargar la información del aula.');
        this.cargando.set(false);
      }
    });
  }

  private actualizarHorario(lista: CursoAsignado[]): void {
    this.horario.set(lista);
    this.horarioConHora.set(lista.filter(c => !!c.horaInicio));
    this.horarioSinHora.set(lista.filter(c => !c.horaInicio));
    this.edicionesHorario = {};
    for (const c of lista.filter(c => !c.horaInicio)) {
      this.edicionesHorario[c.id] = { diaSemana: 'LUNES', horaInicio: '', horaFin: '' };
    }
  }

  edicion(cursoId: number): EdicionHorario {
    return this.edicionesHorario[cursoId] ?? (this.edicionesHorario[cursoId] = { diaSemana: 'LUNES', horaInicio: '', horaFin: '' });
  }

  guardarHorario(curso: CursoAsignado): void {
    const edicion = this.edicion(curso.id);
    if (!edicion.diaSemana || !edicion.horaInicio || !edicion.horaFin) {
      this.errorHorario.set('Complete día, hora de inicio y hora de fin.');
      return;
    }
    this.errorHorario.set('');
    this.guardandoHorarioId.set(curso.id);
    this.cursoAsignadoService.actualizarHorario(curso.id, {
      diaSemana: edicion.diaSemana,
      horaInicio: edicion.horaInicio,
      horaFin: edicion.horaFin
    }).subscribe({
      next: () => {
        this.guardandoHorarioId.set(null);
        this.cursoAsignadoService.listarPorAula(this.aulaId).subscribe({
          next: (lista) => this.actualizarHorario(lista),
          error: () => {}
        });
      },
      error: (err) => {
        this.guardandoHorarioId.set(null);
        this.errorHorario.set(err?.error?.error || 'No se pudo guardar el horario.');
      }
    });
  }

  cambiarPeriodo(p: Periodo): void {
    this.periodo.set(p);
    this.cargarResumen();
  }

  cargarResumen(): void {
    this.aulaService.obtenerResumenAsistencia(this.aulaId, this.periodo(), this.anio, this.mes).subscribe({
      next: (r) => this.resumen.set(r),
      error: (err) => console.error('Error al cargar el resumen de asistencia:', err)
    });
  }

  alturaBarra(alumnos: number): number {
    const buckets = this.resumen()?.buckets ?? [];
    const max = Math.max(1, ...buckets.map(b => b.alumnos));
    return Math.round((alumnos / max) * 120);
  }

  linkWhatsApp(alumno: AlumnoResponse): string | null {
    return generarLinkWhatsApp(alumno.celularApoderado, this.mensajeApoderados);
  }

  volverAlPanel(): void {
    this.router.navigate(['/admin-dashboard']);
  }

  logout(): void {
    this.authService.logout();
  }
}
