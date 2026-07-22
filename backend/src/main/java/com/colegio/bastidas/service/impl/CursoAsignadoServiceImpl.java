package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.dto.curso.CursoAsignadoRequestDTO;
import com.colegio.bastidas.dto.curso.HorarioRequestDTO;
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

import java.time.LocalTime;
import java.util.List;

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

        validarHorarioCompletoOVacio(dto.getDiaSemana(), dto.getHoraInicio(), dto.getHoraFin());

        Docente docente = docenteRepository.findById(dto.getDocenteId())
            .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado"));
        Aula aula = aulaRepository.findById(dto.getAulaId())
            .orElseThrow(() -> new IllegalArgumentException("Aula no encontrada"));

        if (dto.getDiaSemana() != null) {
            validarSinSolapamiento(dto.getAulaId(), dto.getDocenteId(), dto.getDiaSemana(),
                dto.getHoraInicio(), dto.getHoraFin(), dto.getAnioAcademico(), null);
        }

        CursoAsignado curso = CursoAsignado.builder()
            .docente(docente)
            .aula(aula)
            .areaCurricular(dto.getAreaCurricular())
            .anioAcademico(dto.getAnioAcademico())
            .diaSemana(dto.getDiaSemana())
            .horaInicio(dto.getHoraInicio())
            .horaFin(dto.getHoraFin())
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

    @Override
    public List<CursoAsignado> listarPorAula(Long aulaId) {
        return cursoAsignadoRepository.findByAulaId(aulaId);
    }

    @Override
    public CursoAsignado actualizarHorario(Long id, HorarioRequestDTO dto) {
        CursoAsignado curso = cursoAsignadoRepository.findByIdWithDetalles(id)
            .orElseThrow(() -> new IllegalArgumentException("Asignación de curso no encontrada"));

        validarSinSolapamiento(curso.getAula().getId(), curso.getDocente().getId(), dto.getDiaSemana(),
            dto.getHoraInicio(), dto.getHoraFin(), curso.getAnioAcademico(), id);

        curso.setDiaSemana(dto.getDiaSemana());
        curso.setHoraInicio(dto.getHoraInicio());
        curso.setHoraFin(dto.getHoraFin());
        return cursoAsignadoRepository.save(curso);
    }

    // ── Validaciones ──────────────────────────────────────────────────────────

    private void validarHorarioCompletoOVacio(CursoAsignado.DiaSemana dia, LocalTime inicio, LocalTime fin) {
        boolean algunoPresente = dia != null || inicio != null || fin != null;
        boolean todosPresentes = dia != null && inicio != null && fin != null;
        if (algunoPresente && !todosPresentes) {
            throw new IllegalArgumentException("Debe indicar día, hora de inicio y hora de fin juntos.");
        }
        if (todosPresentes && !inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }
    }

    private void validarSinSolapamiento(Long aulaId, Long docenteId, CursoAsignado.DiaSemana dia,
                                         LocalTime inicio, LocalTime fin, Integer anio, Long idExcluir) {
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        for (CursoAsignado c : cursoAsignadoRepository.findByAulaIdAndDiaSemanaAndAnioAcademico(aulaId, dia, anio)) {
            if (!c.getId().equals(idExcluir) && c.getHoraInicio() != null && solapan(c, inicio, fin)) {
                throw new IllegalArgumentException("El aula ya tiene asignada '" + c.getAreaCurricular()
                    + "' de " + c.getHoraInicio() + " a " + c.getHoraFin() + " los " + dia + ".");
            }
        }

        for (CursoAsignado c : cursoAsignadoRepository.findByDocenteIdAndDiaSemanaAndAnioAcademico(docenteId, dia, anio)) {
            if (!c.getId().equals(idExcluir) && c.getHoraInicio() != null && solapan(c, inicio, fin)) {
                throw new IllegalArgumentException("El docente ya dicta en '" + c.getAula().getDescripcion()
                    + "' de " + c.getHoraInicio() + " a " + c.getHoraFin() + " los " + dia + ".");
            }
        }
    }

    private boolean solapan(CursoAsignado existente, LocalTime inicio, LocalTime fin) {
        return existente.getHoraInicio().isBefore(fin) && inicio.isBefore(existente.getHoraFin());
    }
}
