package com.colegio.bastidas.controller;

import com.colegio.bastidas.dto.circular.CircularRequestDTO;
import com.colegio.bastidas.dto.circular.CircularResponseDTO;
import com.colegio.bastidas.service.CircularService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Comunicados oficiales del Director (tablón de anuncios interno).
 * Endpoints base: {@code /api/v1/circulares}
 */
@RestController
@RequestMapping("/circulares")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
public class CircularController {

    private final CircularService circularService;

    /** GET /circulares — DIRECTOR/ADMIN ven todo (borradores y publicadas); DOCENTE/ALUMNO solo lo publicado. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CircularResponseDTO>> listar(Authentication authentication) {
        List<CircularResponseDTO> circulares = circularService.listar();
        boolean esDireccion = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTOR") || a.getAuthority().equals("ROLE_ADMIN"));
        if (!esDireccion) {
            circulares = circulares.stream().filter(CircularResponseDTO::isPublicada).toList();
        }
        return ResponseEntity.ok(circulares);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<CircularResponseDTO> crear(
            @Valid @RequestBody CircularRequestDTO dto, Authentication authentication) {
        return ResponseEntity.ok(circularService.crear(dto, authentication));
    }

    @PutMapping("/{id}/publicar")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ResponseEntity<CircularResponseDTO> publicar(@PathVariable Long id) {
        return ResponseEntity.ok(circularService.publicar(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
