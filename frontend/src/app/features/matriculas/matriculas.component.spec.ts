import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatriculasComponent } from './matriculas.component';
import { MatriculaService } from '../../core/services/matricula.service';
import { of } from 'rxjs';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { signal } from '@angular/core';

describe('MatriculasComponent', () => {
  let component: MatriculasComponent;
  let fixture: ComponentFixture<MatriculasComponent>;
  let matriculaServiceMock: any;

  beforeEach(async () => {
    matriculaServiceMock = {
      matriculas: signal([]),
      loading: signal(false),
      cargarMatriculas: jasmine.createSpy('cargarMatriculas').and.returnValue(of([])),
      crearMatricula: jasmine.createSpy('crearMatricula').and.returnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [MatriculasComponent, ReactiveFormsModule],
      providers: [
        { provide: MatriculaService, useValue: matriculaServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MatriculasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debería crearse exitosamente', () => {
    expect(component).toBeTruthy();
  });

  it('debería inicializar el formulario de matrícula correctamente', () => {
    expect(component.matriculaForm).toBeDefined();
    expect(component.matriculaForm.get('alumnoId')).toBeTruthy();
    expect(component.matriculaForm.get('anioEscolar')?.value).toBe(new Date().getFullYear());
  });

  it('debería mostrar error si se intenta guardar un formulario inválido', () => {
    // Formulario vacío inicialmente (inválido)
    component.guardarMatricula();
    expect(matriculaServiceMock.crearMatricula).not.toHaveBeenCalled();
  });

  it('debería llamar a crearMatricula cuando el formulario es válido', () => {
    component.matriculaForm.setValue({
      alumnoId: 1,
      anioEscolar: 2026,
      grado: '3',
      seccion: 'A'
    });

    component.guardarMatricula();
    expect(matriculaServiceMock.crearMatricula).toHaveBeenCalled();
    expect(component.showModal()).toBeFalse(); // Debería cerrar el modal
  });
});
