import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/** Placeholder del módulo de matrículas – implementación completa en Fase 2. */
@Component({
  selector: 'app-matriculas',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="padding:3rem;text-align:center;color:#64748b">
      <div style="font-size:3rem;margin-bottom:1rem">📋</div>
      <h2 style="color:#1a3a6b;margin-bottom:0.5rem">Módulo de Matrículas</h2>
      <p>En desarrollo – Fase 2</p>
    </div>
  `
})
export class MatriculasComponent {}
