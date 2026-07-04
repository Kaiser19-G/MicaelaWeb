package com.colegio.bastidas.controller;

import com.colegio.bastidas.model.Nota;
import com.colegio.bastidas.service.NotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/notas")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
public class NotaController {

    private final NotaService notaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public ResponseEntity<Nota> registrarNota(@RequestBody Nota nota) {
        return ResponseEntity.ok(notaService.registrarNota(nota));
    }

    @PutMapping("/{notaId}")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public ResponseEntity<Nota> actualizarNota(@PathVariable Long notaId, @RequestBody Nota nota) {
        return ResponseEntity.ok(notaService.actualizarNota(notaId, nota));
    }

    @GetMapping("/alumno/{alumnoId}/anio/{anio}")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN','ALUMNO')")
    public ResponseEntity<List<Nota>> obtenerNotasPorAlumnoYAnio(
            @PathVariable Long alumnoId,
            @PathVariable Integer anio) {
        return ResponseEntity.ok(notaService.obtenerNotasPorAlumnoYAnio(alumnoId, anio));
    }

    @GetMapping("/aula/{aulaId}/area/{area}/periodo/{periodo}/anio/{anio}")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public ResponseEntity<List<Nota>> obtenerNotasPorAulaYArea(
            @PathVariable Long aulaId,
            @PathVariable String area,
            @PathVariable String periodo,
            @PathVariable Integer anio) {
        return ResponseEntity.ok(notaService.obtenerNotasPorAulaYArea(aulaId, area, periodo, anio));
    }

    @PostMapping("/{notaId}/evidencia")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public ResponseEntity<String> adjuntarEvidencia(
            @PathVariable Long notaId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("descripcion") String descripcion) {
        return ResponseEntity.ok(notaService.adjuntarEvidencia(notaId, archivo, descripcion));
    }
}
