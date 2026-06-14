package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.dto.docente.DocenteRequestDTO;
import com.colegio.bastidas.dto.docente.DocenteResponseDTO;
import com.colegio.bastidas.model.Docente;
import com.colegio.bastidas.repository.DocenteRepository;
import com.colegio.bastidas.service.DocenteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio de Docentes.
 *
 * Semáforo Curricular (RN-04):
 *   APROBADO  → El docente ha subido programación anual Y todas las unidades.
 *   PENDIENTE → Subió la programación pero le faltan unidades didácticas.
 *   RETRASADO → No ha subido programación anual (evidencias = 0).
 *
 * NOTA: La lógica real del semáforo se conectará con la tabla de evidencias
 * en Sprint 5. Por ahora se calcula desde el campo cantidadEvidencias.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DocenteServiceImpl implements DocenteService {

    private final DocenteRepository docenteRepository;

    @Override
    public List<DocenteResponseDTO> listarTodos() {
        return docenteRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @Override
    public DocenteResponseDTO buscarPorId(Long id) {
        return docenteRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new RuntimeException("Docente no encontrado con ID: " + id));
    }

    @Override
    public DocenteResponseDTO buscarPorDni(String dni) {
        return docenteRepository.findByDni(dni)
            .map(this::toDTO)
            .orElseThrow(() -> new RuntimeException("Docente no encontrado con DNI: " + dni));
    }

    @Override
    @Transactional
    public DocenteResponseDTO crear(DocenteRequestDTO dto) {
        if (docenteRepository.existsByDni(dto.getDni())) {
            throw new IllegalArgumentException("Ya existe un docente con DNI: " + dto.getDni());
        }
        Docente docente = Docente.builder()
            .codigoDocente(dto.getCodigoDocente())
            .dni(dto.getDni())
            .apellidoPaterno(dto.getApellidoPaterno())
            .apellidoMaterno(dto.getApellidoMaterno())
            .nombres(dto.getNombres())
            .especialidad(dto.getEspecialidad())
            .condicion(dto.getCondicion() != null
                ? Docente.Condicion.valueOf(dto.getCondicion())
                : Docente.Condicion.NOMBRADO)
            .emailInstitucional(dto.getEmailInstitucional())
            .celular(dto.getCelular())
            .build();
        Docente guardado = docenteRepository.save(docente);
        log.info("Docente creado: {} (DNI: {})", guardado.getNombreCompleto(), guardado.getDni());
        return toDTO(guardado);
    }

    @Override
    @Transactional
    public DocenteResponseDTO actualizar(Long id, DocenteRequestDTO dto) {
        Docente docente = docenteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Docente no encontrado con ID: " + id));

        docente.setApellidoPaterno(dto.getApellidoPaterno());
        docente.setApellidoMaterno(dto.getApellidoMaterno());
        docente.setNombres(dto.getNombres());
        docente.setEspecialidad(dto.getEspecialidad());
        if (dto.getCondicion() != null) {
            docente.setCondicion(Docente.Condicion.valueOf(dto.getCondicion()));
        }
        docente.setEmailInstitucional(dto.getEmailInstitucional());
        docente.setCelular(dto.getCelular());

        return toDTO(docenteRepository.save(docente));
    }

    @Override
    public List<DocenteResponseDTO> obtenerSemaforoCurricular() {
        // Todos los docentes con su estado curricular calculado
        return docenteRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @Override
    public long contarPorEstadoCurricular(String estado) {
        // En Sprint 5 se conectará con la tabla de evidencias/programaciones
        return docenteRepository.findAll().stream()
            .filter(d -> calcularEstadoCurricular(d).equalsIgnoreCase(estado))
            .count();
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private DocenteResponseDTO toDTO(Docente d) {
        DocenteResponseDTO dto = DocenteResponseDTO.fromEntity(d);
        // Calcular estado curricular dinámicamente
        dto.setEstadoCurricular(calcularEstadoCurricular(d));
        return dto;
    }

    /**
     * Lógica del Semáforo Curricular (RN-04).
     * Se expande en Sprint 5 con la tabla de programaciones.
     * Por ahora: determina estado desde la cantidad de evidencias del docente.
     */
    private String calcularEstadoCurricular(Docente d) {
        // Placeholder: Se reemplaza por consulta real a tabla de programaciones en Sprint 5
        // La lógica real: verificar subida de programación anual + unidades didácticas
        return "PENDIENTE"; // valor por defecto hasta integrar tabla de programaciones
    }
}
