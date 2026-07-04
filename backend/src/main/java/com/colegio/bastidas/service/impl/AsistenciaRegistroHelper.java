package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.model.Asistencia;
import com.colegio.bastidas.model.Asistencia.EstadoAsistencia;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.AsistenciaRepository;
import com.colegio.bastidas.repository.DocenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Helper que ejecuta cada registro de asistencia en su propia transacción
 * independiente (REQUIRES_NEW), de modo que un fallo de constraint en un alumno
 * no envenene la sesión JPA del lote completo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsistenciaRegistroHelper {

    private final AsistenciaRepository asistenciaRepository;
    private final AlumnoRepository alumnoRepository;
    private final DocenteRepository docenteRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Asistencia registrarUno(Long alumnoId, LocalDate fecha,
                                   EstadoAsistencia estado, Long docenteId,
                                   String justificacion) {

        // Prevenir duplicados – si ya existe lo retornamos actualizado
        if (asistenciaRepository.existsByAlumnoIdAndFecha(alumnoId, fecha)) {
            Asistencia existente = asistenciaRepository
                .findByAlumnoIdAndFecha(alumnoId, fecha).orElseThrow();
            // Actualizar estado si cambió
            existente.setEstado(estado);
            if (justificacion != null) {
                existente.setJustificacion(justificacion);
                existente.setTieneJustificacion(true);
            }
            return asistenciaRepository.save(existente);
        }

        Alumno alumno = alumnoRepository.findById(alumnoId)
            .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado: " + alumnoId));

        // Lógica de permiso de academia
        boolean aplicarPermiso = false;
        String horaPermiso = null;
        EstadoAsistencia estadoFinal = estado;
        if (Boolean.TRUE.equals(alumno.getTienePermisoAcademia())
                && estado == EstadoAsistencia.TARDANZA) {
            aplicarPermiso = true;
            horaPermiso = alumno.getHoraEntradaAcademia();
            estadoFinal = EstadoAsistencia.PERMISO_ACADEMIA;
        }

        Asistencia asistencia = Asistencia.builder()
            .alumno(alumno)
            .aula(alumno.getAula())   // <-- heredar aula del alumno
            .fecha(fecha)
            .estado(estadoFinal)
            .docente(docenteRepository.findById(docenteId).orElse(null))
            .aplicadoPermisoAcademia(aplicarPermiso)
            .horaPermisoAcademia(horaPermiso)
            .tieneJustificacion(justificacion != null)
            .justificacion(justificacion)
            .build();

        return asistenciaRepository.save(asistencia);
    }
}
