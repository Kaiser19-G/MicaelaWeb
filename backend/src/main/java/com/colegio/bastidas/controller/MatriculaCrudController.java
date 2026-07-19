package com.colegio.bastidas.controller;

import com.colegio.bastidas.dto.MatriculaDto;
import com.colegio.bastidas.exception.AulaCompletaException;
import com.colegio.bastidas.service.MatriculaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/matriculas-crud")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
public class MatriculaCrudController {

    private final MatriculaService matriculaService;

    public MatriculaCrudController(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    @ExceptionHandler(AulaCompletaException.class)
    public ResponseEntity<Map<String, String>> manejarAulaCompleta(AulaCompletaException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @GetMapping("/anio/{anio}")
    public ResponseEntity<List<MatriculaDto>> listarPorAnio(@PathVariable Integer anio) {
        return ResponseEntity.ok(matriculaService.listarPorAnio(anio));
    }

    @PostMapping
    public ResponseEntity<MatriculaDto> crear(@Valid @RequestBody MatriculaDto dto) {
        return new ResponseEntity<>(matriculaService.crearMatricula(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatriculaDto> actualizar(@PathVariable Long id, @Valid @RequestBody MatriculaDto dto) {
        return ResponseEntity.ok(matriculaService.actualizarMatricula(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        matriculaService.eliminarMatricula(id);
        return ResponseEntity.noContent().build();
    }
}
