import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatriculaService, MatriculaDto } from '../../core/services/matricula.service';

@Component({
  selector: 'app-matriculas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './matriculas.component.html',
  styleUrls: ['./matriculas.component.scss']
})
export class MatriculasComponent implements OnInit {
  private matriculaService = inject(MatriculaService);

  anioActual = new Date().getFullYear();
  matriculas = signal<MatriculaDto[]>([]);
  loading = signal(false);

  // Modal form
  showModal = signal(false);
  isEdit = signal(false);
  formMatricula: MatriculaDto = this.getEmptyForm();

  ngOnInit() {
    this.cargarMatriculas();
  }

  cargarMatriculas() {
    this.loading.set(true);
    this.matriculaService.listarPorAnio(this.anioActual).subscribe({
      next: (data) => {
        this.matriculas.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error cargando matrículas', err);
        // Fallback mock
        this.matriculas.set([
          { id: 1, alumnoId: 101, nombreAlumno: 'Juan Perez', codigoAlumno: 'IE-MB-2026-12345678', grado: '1ro Secundaria', seccion: 'A', anioEscolar: 2026, estado: 'ACTIVO' }
        ]);
        this.loading.set(false);
      }
    });
  }

  abrirModalNuevo() {
    this.isEdit.set(false);
    this.formMatricula = this.getEmptyForm();
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

  guardar() {
    if (this.isEdit() && this.formMatricula.id) {
      this.matriculaService.actualizar(this.formMatricula.id, this.formMatricula).subscribe({
        next: () => {
          this.cargarMatriculas();
          this.cerrarModal();
        },
        error: (err) => console.error(err)
      });
    } else {
      this.matriculaService.crear(this.formMatricula).subscribe({
        next: () => {
          this.cargarMatriculas();
          this.cerrarModal();
        },
        error: (err) => console.error(err)
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

  private getEmptyForm(): MatriculaDto {
    return {
      alumnoId: 0,
      grado: '',
      seccion: '',
      anioEscolar: this.anioActual,
      estado: 'ACTIVO'
    };
  }
}
