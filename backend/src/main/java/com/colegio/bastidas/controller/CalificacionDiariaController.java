package com.colegio.bastidas.controller;

import com.colegio.bastidas.dto.CalificacionDiariaRequestDTO;
import com.colegio.bastidas.model.CalificacionDiaria;
import com.colegio.bastidas.service.CalificacionDiariaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/calificaciones-diarias")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
public class CalificacionDiariaController {

    private final CalificacionDiariaService calificacionService;

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public ResponseEntity<List<CalificacionDiaria>> guardarLote(
            @RequestParam Long cursoAsignadoId,
            @RequestParam Integer semana,
            @RequestParam Long docenteId,
            @RequestBody List<CalificacionDiariaRequestDTO> calificaciones) {
        
        return ResponseEntity.ok(calificacionService.guardarLote(cursoAsignadoId, semana, docenteId, calificaciones));
    }

    @GetMapping("/curso/{cursoAsignadoId}/semana/{semana}")
    @PreAuthorize("hasAnyRole('DOCENTE','ALUMNO','ADMIN')")
    public ResponseEntity<List<CalificacionDiaria>> obtenerPorCursoYSemana(
            @PathVariable Long cursoAsignadoId,
            @PathVariable Integer semana) {
        
        return ResponseEntity.ok(calificacionService.obtenerPorCursoYSemana(cursoAsignadoId, semana));
    }
}
