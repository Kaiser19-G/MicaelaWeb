import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';

/** Componente raíz de la aplicación Angular 19 Standalone. */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  template: `<router-outlet />`,
  styles: [`
    :host { display: block; }
  `]
})
export class AppComponent {
  title = 'I.E. Micaela Bastidas – Sistema de Gestión Escolar';
}
