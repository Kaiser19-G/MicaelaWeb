package com.colegio.bastidas.controller;

import com.colegio.bastidas.model.MaterialSemana;
import com.colegio.bastidas.service.MaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/materiales")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class MaterialController {

    private final MaterialService materialService;
    private final RestTemplate restTemplate;

    // ── Subir material ─────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public ResponseEntity<Map<String, Object>> subirMaterial(
            @RequestParam Long cursoAsignadoId,
            @RequestParam Integer semana,
            @RequestParam Long docenteId,
            @RequestParam("archivo") MultipartFile archivo) {

        MaterialSemana material = materialService.subirMaterial(
            cursoAsignadoId, semana, archivo, docenteId);
        return ResponseEntity.ok(toDto(material));
    }

    // ── Listar materiales ──────────────────────────────────────────────────

    @GetMapping("/curso/{cursoAsignadoId}/semana/{semana}")
    @PreAuthorize("hasAnyRole('DOCENTE','ALUMNO','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerMateriales(
            @PathVariable Long cursoAsignadoId,
            @PathVariable Integer semana) {

        return ResponseEntity.ok(
            materialService.obtenerMaterialesPorCursoYSemana(cursoAsignadoId, semana)
                .stream().map(this::toDto).toList());
    }

    @GetMapping("/curso/{cursoAsignadoId}")
    @PreAuthorize("hasAnyRole('DOCENTE','ALUMNO','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> obtenerMaterialesPorCurso(
            @PathVariable Long cursoAsignadoId) {

        return ResponseEntity.ok(
            materialService.obtenerMaterialesPorCurso(cursoAsignadoId)
                .stream().map(this::toDto).toList());
    }

    // ── Descargar material (proxy) ─────────────────────────────────────────
    /**
     * GET /materiales/{materialId}/descargar
     *
     * Actúa como proxy hacia Supabase Storage y fuerza la descarga
     * añadiendo Content-Disposition: attachment; filename="<nombre original>".
     * Esto evita que el navegador abra el archivo como HTML.
     */
    @GetMapping("/{materialId}/descargar")
    @PreAuthorize("hasAnyRole('DOCENTE','ALUMNO','ADMIN')")
    public ResponseEntity<byte[]> descargarMaterial(@PathVariable Long materialId) {
        MaterialSemana material = materialService.obtenerPorId(materialId);

        try {
            // Obtener el archivo desde Supabase Storage
            ResponseEntity<byte[]> response = restTemplate.exchange(
                URI.create(material.getUrlArchivo()),
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                byte[].class
            );

            byte[] bytes = response.getBody();
            if (bytes == null) bytes = new byte[0];

            // Construir headers de respuesta con descarga forzada
            HttpHeaders headers = new HttpHeaders();
            String nombreArchivo = material.getNombreArchivo() != null
                ? material.getNombreArchivo() : "archivo";

            headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename(nombreArchivo, java.nio.charset.StandardCharsets.UTF_8)
                    .build()
            );

            // Detectar Content-Type del archivo original
            MediaType mediaType = detectarMediaType(nombreArchivo);
            headers.setContentType(mediaType);
            headers.setContentLength(bytes.length);

            log.info("Descarga de material id={}, archivo={}", materialId, nombreArchivo);
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error al descargar material id={}: {}", materialId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    // ── Eliminar material ──────────────────────────────────────────────────

    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    public ResponseEntity<Void> eliminarMaterial(@PathVariable Long materialId) {
        materialService.eliminarMaterial(materialId);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Serializa MaterialSemana a un Map simple sin entidades JPA anidadas. */
    private Map<String, Object> toDto(MaterialSemana m) {
        return Map.of(
            "id", m.getId(),
            "semana", m.getSemana(),
            "nombreArchivo", m.getNombreArchivo() != null ? m.getNombreArchivo() : "",
            "urlArchivo", m.getUrlArchivo() != null ? m.getUrlArchivo() : "",
            "urlDescarga", "/api/v1/materiales/" + m.getId() + "/descargar",
            "fechaSubida", m.getCreatedAt() != null ? m.getCreatedAt().toLocalDate().toString() : ""
        );
    }

    /** Detecta el MediaType basándose en la extensión del archivo. */
    private MediaType detectarMediaType(String nombreArchivo) {
        String nombre = nombreArchivo.toLowerCase();
        if (nombre.endsWith(".pdf"))  return MediaType.APPLICATION_PDF;
        if (nombre.endsWith(".doc"))  return MediaType.parseMediaType("application/msword");
        if (nombre.endsWith(".docx")) return MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        if (nombre.endsWith(".xls"))  return MediaType.parseMediaType("application/vnd.ms-excel");
        if (nombre.endsWith(".xlsx")) return MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        if (nombre.endsWith(".ppt"))  return MediaType.parseMediaType("application/vnd.ms-powerpoint");
        if (nombre.endsWith(".pptx")) return MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        if (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (nombre.endsWith(".png"))  return MediaType.IMAGE_PNG;
        if (nombre.endsWith(".zip"))  return MediaType.parseMediaType("application/zip");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
