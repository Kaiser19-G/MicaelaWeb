package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.dto.CalificacionDiariaRequestDTO;
import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.model.CalificacionDiaria;
import com.colegio.bastidas.model.CursoAsignado;
import com.colegio.bastidas.model.Docente;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.CalificacionDiariaRepository;
import com.colegio.bastidas.repository.CursoAsignadoRepository;
import com.colegio.bastidas.repository.DocenteRepository;
import com.colegio.bastidas.service.CalificacionDiariaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CalificacionDiariaServiceImpl implements CalificacionDiariaService {

    private final CalificacionDiariaRepository calificacionRepository;
    private final CursoAsignadoRepository cursoAsignadoRepository;
    private final DocenteRepository docenteRepository;
    private final AlumnoRepository alumnoRepository;

    @Override
    public List<CalificacionDiaria> guardarLote(Long cursoAsignadoId, Integer semana, Long docenteId, List<CalificacionDiariaRequestDTO> calificacionesDto) {
        Docente docente = docenteRepository.findById(docenteId)
                .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado"));

        CursoAsignado curso = cursoAsignadoRepository.findById(cursoAsignadoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado"));

        List<CalificacionDiaria> guardadas = new ArrayList<>();

        for (CalificacionDiariaRequestDTO dto : calificacionesDto) {
            Alumno alumno = alumnoRepository.findById(dto.getAlumnoId())
                    .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));

            Optional<CalificacionDiaria> existente = calificacionRepository
                    .findByAlumnoIdAndCursoAsignadoId(alumno.getId(), curso.getId())
                    .stream()
                    .filter(c -> c.getSemana().equals(semana))
                    .findFirst();

            CalificacionDiaria calificacion = existente.orElse(CalificacionDiaria.builder()
                    .alumno(alumno)
                    .cursoAsignado(curso)
                    .semana(semana)
                    .docente(docente)
                    .build());

            calificacion.setCalificacion(dto.getCalificacion());
            guardadas.add(calificacionRepository.save(calificacion));
        }

        return guardadas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalificacionDiaria> obtenerPorCursoYSemana(Long cursoAsignadoId, Integer semana) {
        return calificacionRepository.findByCursoAsignadoIdAndSemana(cursoAsignadoId, semana);
    }
}
