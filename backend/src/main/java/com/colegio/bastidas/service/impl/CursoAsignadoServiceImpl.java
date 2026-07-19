package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.dto.curso.CursoAsignadoRequestDTO;
import com.colegio.bastidas.model.Aula;
import com.colegio.bastidas.model.CursoAsignado;
import com.colegio.bastidas.model.Docente;
import com.colegio.bastidas.repository.AulaRepository;
import com.colegio.bastidas.repository.CursoAsignadoRepository;
import com.colegio.bastidas.repository.DocenteRepository;
import com.colegio.bastidas.service.CursoAsignadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CursoAsignadoServiceImpl implements CursoAsignadoService {

    private final CursoAsignadoRepository cursoAsignadoRepository;
    private final DocenteRepository docenteRepository;
    private final AulaRepository aulaRepository;

    @Override
    public CursoAsignado crear(CursoAsignadoRequestDTO dto) {
        if (cursoAsignadoRepository.existsByAulaIdAndAreaCurricularAndAnioAcademico(
                dto.getAulaId(), dto.getAreaCurricular(), dto.getAnioAcademico())) {
            throw new IllegalArgumentException(
                "Ya existe un docente asignado a " + dto.getAreaCurricular()
                    + " en esta aula para el año " + dto.getAnioAcademico());
        }

        Docente docente = docenteRepository.findById(dto.getDocenteId())
            .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado"));
        Aula aula = aulaRepository.findById(dto.getAulaId())
            .orElseThrow(() -> new IllegalArgumentException("Aula no encontrada"));

        CursoAsignado curso = CursoAsignado.builder()
            .docente(docente)
            .aula(aula)
            .areaCurricular(dto.getAreaCurricular())
            .anioAcademico(dto.getAnioAcademico())
            .build();

        CursoAsignado guardado = cursoAsignadoRepository.save(curso);
        log.info("Curso asignado: docente={} aula={} area={}",
            docente.getNombreCompleto(), aula.getDescripcion(), dto.getAreaCurricular());
        return guardado;
    }

    @Override
    public void eliminar(Long id) {
        cursoAsignadoRepository.deleteById(id);
    }
}
