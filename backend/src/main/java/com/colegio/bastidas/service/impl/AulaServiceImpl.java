package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.dto.aula.AulaRequestDTO;
import com.colegio.bastidas.dto.aula.AulaResponseDTO;
import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.model.Aula;
import com.colegio.bastidas.model.Docente;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.AulaRepository;
import com.colegio.bastidas.repository.CursoAsignadoRepository;
import com.colegio.bastidas.repository.DocenteRepository;
import com.colegio.bastidas.service.AulaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio de Aulas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AulaServiceImpl implements AulaService {

    private final AulaRepository aulaRepository;
    private final AlumnoRepository alumnoRepository;
    private final DocenteRepository docenteRepository;
    private final CursoAsignadoRepository cursoAsignadoRepository;

    @Override
    public List<AulaResponseDTO> listarPorAnio(Integer anio) {
        return aulaRepository.findByAnioAcademico(anio).stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public List<AulaResponseDTO> listarPorNivelYAnio(String nivel, Integer anio) {
        Aula.Nivel nivelEnum = Aula.Nivel.valueOf(nivel);
        return aulaRepository.findByNivelAndAnioAcademico(nivelEnum, anio).stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public AulaResponseDTO buscarPorId(Long id) {
        Aula aula = aulaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Aula no encontrada con ID: " + id));
        return toDto(aula);
    }

    @Override
    public long contarPorAnio(Integer anio) {
        return aulaRepository.countByAnioAcademico(anio);
    }

    @Override
    @Transactional
    public AulaResponseDTO crear(AulaRequestDTO dto) {
        Aula.Nivel nivel = Aula.Nivel.valueOf(dto.getNivel().toUpperCase());

        if (nivel == Aula.Nivel.SECUNDARIA && !dto.getSeccion().matches("[A-E]")) {
            throw new IllegalArgumentException(
                "En secundaria la sección debe ser una letra entre A y E.");
        }

        Docente docentePrincipal = null;
        if (dto.getDocentePrincipalId() != null) {
            docentePrincipal = docenteRepository.findById(dto.getDocentePrincipalId())
                .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado"));
        }

        Aula aula = Aula.builder()
            .grado(dto.getGrado())
            .seccion(dto.getSeccion())
            .nivel(nivel)
            .anioAcademico(dto.getAnioAcademico())
            .capacidad(dto.getCapacidad())
            .aulaReferencia(dto.getAulaReferencia())
            .docentePrincipal(docentePrincipal)
            .build();

        return toDto(aulaRepository.save(aula));
    }

    @Override
    public int contarVacantes(Long aulaId) {
        Aula aula = aulaRepository.findById(aulaId)
            .orElseThrow(() -> new IllegalArgumentException("Aula no encontrada con ID: " + aulaId));
        int capacidad = aula.getCapacidad() != null ? aula.getCapacidad() : 0;
        long activos = alumnoRepository.countByAulaIdAndEstadoMatricula(aulaId, Alumno.EstadoMatricula.ACTIVO);
        return (int) Math.max(0, capacidad - activos);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Aula aula = aulaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Aula no encontrada con ID: " + id));

        long activos = alumnoRepository.countByAulaIdAndEstadoMatricula(id, Alumno.EstadoMatricula.ACTIVO);
        if (activos > 0) {
            throw new IllegalArgumentException(
                "No se puede eliminar: el aula tiene " + activos + " alumno(s) matriculado(s). "
                    + "Reasígnelos a otra aula primero.");
        }

        if (!cursoAsignadoRepository.findByAulaId(id).isEmpty()) {
            throw new IllegalArgumentException(
                "No se puede eliminar: el aula tiene cursos asignados. Elimínelos primero.");
        }

        try {
            aulaRepository.delete(aula);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new IllegalArgumentException(
                "No se puede eliminar: el aula tiene registros históricos asociados (matrículas anteriores).");
        }
        log.info("Aula eliminada: {}", aula.getDescripcion());
    }

    private AulaResponseDTO toDto(Aula aula) {
        long activos = alumnoRepository.countByAulaIdAndEstadoMatricula(aula.getId(), Alumno.EstadoMatricula.ACTIVO);
        return AulaResponseDTO.fromEntity(aula, (int) activos);
    }
}
