package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.dto.calidad.AulaValidacionDTO;
import com.colegio.bastidas.dto.calidad.CalidadResumenDTO;
import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.model.Aula;
import com.colegio.bastidas.model.CursoAsignado;
import com.colegio.bastidas.model.Nota;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.AulaRepository;
import com.colegio.bastidas.repository.CursoAsignadoRepository;
import com.colegio.bastidas.repository.NotaRepository;
import com.colegio.bastidas.service.CalidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalidadServiceImpl implements CalidadService {

    private final AulaRepository aulaRepository;
    private final AlumnoRepository alumnoRepository;
    private final CursoAsignadoRepository cursoAsignadoRepository;
    private final NotaRepository notaRepository;

    @Override
    public List<AulaValidacionDTO> listarValidacionAulas(Integer anio) {
        return aulaRepository.findByAnioAcademico(anio).stream()
            .map(this::validarAula)
            .toList();
    }

    @Override
    public CalidadResumenDTO obtenerResumen(Integer anio) {
        List<AulaValidacionDTO> aulas = listarValidacionAulas(anio);

        int estudiantesTotal = aulas.stream().mapToInt(AulaValidacionDTO::getEstudiantes).sum();
        int estudiantesConNotas = aulas.stream().mapToInt(AulaValidacionDTO::getNotasCompletas).sum();

        return CalidadResumenDTO.builder()
            .totalAulas(aulas.size())
            .aulasOk((int) aulas.stream().filter(a -> "ok".equals(a.getEstado())).count())
            .conAdvertencias((int) aulas.stream().filter(a -> "advertencia".equals(a.getEstado())).count())
            .conErrores((int) aulas.stream().filter(a -> "error".equals(a.getEstado())).count())
            .estudiantesConNotas(estudiantesConNotas)
            .estudiantesTotal(estudiantesTotal)
            .build();
    }

    private AulaValidacionDTO validarAula(Aula aula) {
        int estudiantes = (int) alumnoRepository.countByAulaIdAndEstadoMatricula(aula.getId(), Alumno.EstadoMatricula.ACTIVO);
        List<CursoAsignado> cursos = cursoAsignadoRepository.findByAulaId(aula.getId());
        List<Nota> notas = notaRepository.findByAulaIdAndAnioAcademico(aula.getId(), aula.getAnioAcademico());

        Set<Long> alumnosConNotas = notas.stream().map(n -> n.getAlumno().getId()).collect(Collectors.toSet());
        int notasCompletas = alumnosConNotas.size();
        int notasBlanco = Math.max(0, estudiantes - notasCompletas);

        Set<String> paresDocenteArea = cursos.stream()
            .map(c -> c.getDocente().getId() + "|" + c.getAreaCurricular())
            .collect(Collectors.toSet());
        long inconsistencias = notas.stream()
            .filter(n -> !paresDocenteArea.contains(n.getDocente().getId() + "|" + n.getAreaCurricular()))
            .count();

        String estado;
        if (cursos.isEmpty() || notasBlanco > estudiantes / 2.0) {
            estado = "error";
        } else if (notasBlanco > 0 || inconsistencias > 0) {
            estado = "advertencia";
        } else {
            estado = "ok";
        }

        return AulaValidacionDTO.builder()
            .aulaId(aula.getId())
            .nombre(aula.getDescripcion())
            .estado(estado)
            .estudiantes(estudiantes)
            .notasCompletas(notasCompletas)
            .notasBlanco(notasBlanco)
            .inconsistencias((int) inconsistencias)
            .build();
    }
}
