import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatriculaService, MatriculaDto } from '../../core/services/matricula.service';
import { AulaService, AulaResponseDTO } from '../../core/services/aula.service';
import { AlumnoService, AlumnoResponse } from '../../core/services/alumno.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-matriculas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './matriculas.component.html',
  styleUrls: ['./matriculas.component.scss']
})
export class MatriculasComponent implements OnInit {
  private matriculaService = inject(MatriculaService);
  private aulaService = inject(AulaService);
  private alumnoService = inject(AlumnoService);
  private router = inject(Router);
  private authService = inject(AuthService);

  anioActual = new Date().getFullYear();
  matriculas = signal<MatriculaDto[]>([]);
  aulas = signal<AulaResponseDTO[]>([]);
  loading = signal(false);
  error = signal('');

  // Modal form
  showModal = signal(false);
  isEdit = signal(false);
  formMatricula: MatriculaDto = this.getEmptyForm();

  // Búsqueda de alumno (matrícula nueva / reinscripción de un alumno ya existente)
  dniBusqueda = '';
  buscandoAlumno = signal(false);
  alumnoEncontrado = signal<AlumnoResponse | null>(null);
  errorBusquedaAlumno = signal('');

  ngOnInit() {
    this.cargarMatriculas();
    this.cargarAulas();
  }

  cargarMatriculas() {
    this.loading.set(true);
    this.error.set('');
    this.matriculaService.listarPorAnio(this.anioActual).subscribe({
      next: (data) => {
        this.matriculas.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error cargando matrículas', err);
        this.error.set('No se pudieron cargar las matrículas. Intente nuevamente.');
        this.loading.set(false);
      }
    });
  }

  cargarAulas() {
    this.aulaService.listarPorAnio(this.anioActual).subscribe({
      next: (data) => this.aulas.set(data),
      error: (err) => console.error('Error cargando aulas', err)
    });
  }

  /** Vacantes disponibles del aula actualmente seleccionada en el formulario. */
  get vacantesAulaSeleccionada(): number | null {
    const aula = this.aulas().find(a => a.id === this.formMatricula.aulaId);
    return aula ? aula.vacantesDisponibles : null;
  }

  abrirModalNuevo() {
    this.isEdit.set(false);
    this.formMatricula = this.getEmptyForm();
    this.dniBusqueda = '';
    this.alumnoEncontrado.set(null);
    this.errorBusquedaAlumno.set('');
    this.showModal.set(true);
  }

  abrirModalEditar(m: MatriculaDto) {
    this.isEdit.set(true);
    this.formMatricula = { ...m };
    this.showModal.set(true);
  }

  cerrarModal() {
    this.showModal.set(false);
  }

  /** Busca un alumno ya registrado por DNI (matrícula nueva o reinscripción). */
  buscarAlumnoPorDni() {
    const dni = this.dniBusqueda.trim();
    if (dni.length !== 8) {
      this.errorBusquedaAlumno.set('Ingrese un DNI válido de 8 dígitos.');
      return;
    }
    this.errorBusquedaAlumno.set('');
    this.buscandoAlumno.set(true);
    this.alumnoEncontrado.set(null);
    this.formMatricula.alumnoId = 0;
    this.alumnoService.buscarPorDni(dni).subscribe({
      next: (alumno) => {
        this.buscandoAlumno.set(false);
        this.alumnoEncontrado.set(alumno);
        this.formMatricula.alumnoId = alumno.id;
      },
      error: () => {
        this.buscandoAlumno.set(false);
        this.errorBusquedaAlumno.set(
          'No existe ningún alumno con ese DNI. Debe registrarlo primero desde "+ Nuevo Alumno" en el Panel de Dirección.');
      }
    });
  }

  guardar() {
    this.error.set('');
    if (!this.isEdit() && !this.formMatricula.alumnoId) {
      this.error.set('Busque y seleccione un alumno antes de continuar.');
      return;
    }
    if (this.isEdit() && this.formMatricula.id) {
      this.matriculaService.actualizar(this.formMatricula.id, this.formMatricula).subscribe({
        next: () => {
          this.cargarMatriculas();
          this.cerrarModal();
        },
        error: (err) => this.error.set(err?.error?.error || 'Error al actualizar la matrícula.')
      });
    } else {
      this.matriculaService.crear(this.formMatricula).subscribe({
        next: () => {
          this.cargarMatriculas();
          this.cerrarModal();
        },
        error: (err) => this.error.set(err?.error?.error || 'Error al crear la matrícula.')
      });
    }
  }

  eliminar(id: number) {
    if (confirm('¿Está seguro de eliminar esta matrícula?')) {
      this.matriculaService.eliminar(id).subscribe({
        next: () => this.cargarMatriculas(),
        error: (err) => console.error(err)
      });
    }
  }

  cerrar(): void {
    this.router.navigate(['/admin-dashboard']);
  }

  logout(): void {
    this.authService.logout();
  }

  private getEmptyForm(): MatriculaDto {
    return {
      alumnoId: 0,
      aulaId: 0,
      anioEscolar: this.anioActual,
      estado: 'ACTIVO'
    };
  }
}
