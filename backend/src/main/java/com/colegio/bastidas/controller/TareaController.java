package com.colegio.bastidas.controller;

import com.colegio.bastidas.model.EntregaTarea;
import com.colegio.bastidas.model.Tarea;
import com.colegio.bastidas.service.TareaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/tareas")
@RequiredArgsConstructor
public class TareaController {

    private final TareaService tareaService;

    @PostMapping
    public ResponseEntity<Tarea> crearTarea(
            @RequestParam Long cursoAsignadoId,
            @RequestParam Integer semana,
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaLimite,
            @RequestParam Long docenteId) {
        
        Tarea tarea = tareaService.crearTarea(cursoAsignadoId, semana, titulo, descripcion, fechaLimite, docenteId);
        return ResponseEntity.ok(tarea);
    }

    @GetMapping("/curso/{cursoAsignadoId}/semana/{semana}")
    public ResponseEntity<List<Tarea>> obtenerTareas(
            @PathVariable Long cursoAsignadoId,
            @PathVariable Integer semana) {
        
        return ResponseEntity.ok(tareaService.obtenerTareasPorCursoYSemana(cursoAsignadoId, semana));
    }

    @PostMapping("/{tareaId}/entregas")
    public ResponseEntity<EntregaTarea> subirEntrega(
            @PathVariable Long tareaId,
            @RequestParam Long alumnoId,
            @RequestParam("archivo") MultipartFile archivo) {
        
        EntregaTarea entrega = tareaService.subirEntrega(tareaId, alumnoId, archivo);
        return ResponseEntity.ok(entrega);
    }

    @GetMapping("/{tareaId}/entregas")
    public ResponseEntity<List<EntregaTarea>> obtenerEntregas(@PathVariable Long tareaId) {
        return ResponseEntity.ok(tareaService.obtenerEntregasPorTarea(tareaId));
    }

    @PatchMapping("/entregas/{entregaId}/calificar")
    public ResponseEntity<EntregaTarea> calificarEntrega(
            @PathVariable Long entregaId,
            @RequestParam(required = false) Double nota,
            @RequestParam(required = false) String comentario) {
        
        EntregaTarea entrega = tareaService.calificarEntrega(entregaId, nota, comentario);
        return ResponseEntity.ok(entrega);
    }
}
