package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.dto.alumno.AlumnoCreadoResponseDTO;
import com.colegio.bastidas.dto.alumno.AlumnoRequestDTO;
import com.colegio.bastidas.dto.alumno.AlumnoResponseDTO;
import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.model.Aula;
import com.colegio.bastidas.model.Usuario;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.AsistenciaRepository;
import com.colegio.bastidas.repository.DocenteRepository;
import com.colegio.bastidas.service.AlumnoService;
import com.colegio.bastidas.util.CodigosGenerator;
import com.colegio.bastidas.util.CredencialesGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementación del servicio de Alumnos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AlumnoServiceImpl implements AlumnoService {

    private final AlumnoRepository alumnoRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final UsuarioProvisioningHelper usuarioProvisioningHelper;
    private final DocenteRepository docenteRepository;

    @Override
    public List<AlumnoResponseDTO> listarActivosPorAnio(Integer anio) {
        return alumnoRepository
            .findByAnioAcademicoAndEstadoMatricula(anio, Alumno.EstadoMatricula.ACTIVO)
            .stream()
            .map(AlumnoResponseDTO::fromEntity)
            .toList();
    }

    @Override
    public List<AlumnoResponseDTO> listarPorAula(Long aulaId) {
        return alumnoRepository
            .findByAulaIdAndEstadoMatricula(aulaId, Alumno.EstadoMatricula.ACTIVO)
            .stream()
            .map(AlumnoResponseDTO::fromEntity)
            .toList();
    }

    @Override
    public List<AlumnoResponseDTO> buscar(String termino) {
        return alumnoRepository.buscarPorNombreODni(termino)
            .stream()
            .map(AlumnoResponseDTO::fromEntity)
            .toList();
    }

    @Override
    public AlumnoResponseDTO buscarPorId(Long id) {
        return alumnoRepository.findById(id)
            .map(AlumnoResponseDTO::fromEntity)
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado con ID: " + id));
    }

    @Override
    public AlumnoResponseDTO buscarPorDni(String dni) {
        return alumnoRepository.findByDni(dni)
            .map(AlumnoResponseDTO::fromEntity)
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado con DNI: " + dni));
    }

    @Override
    @Transactional
    public AlumnoCreadoResponseDTO crear(AlumnoRequestDTO dto) {
        if (alumnoRepository.existsByDni(dto.getDni())) {
            throw new IllegalArgumentException("Ya existe un alumno con DNI: " + dto.getDni());
        }

        Usuario usuario = usuarioProvisioningHelper.crearUsuario(
            dto.getDni(), dto.getApellidoPaterno(), Usuario.Rol.ALUMNO);

        Alumno.AlumnoBuilder builder = Alumno.builder()
            .usuario(usuario)
            .codigoEstudiante(CodigosGenerator.generarCodigoEstudiante(dto.getDni(), dto.getAnioAcademico()))
            .dni(dto.getDni())
            .apellidoPaterno(dto.getApellidoPaterno())
            .apellidoMaterno(dto.getApellidoMaterno())
            .nombres(dto.getNombres())
            .anioAcademico(dto.getAnioAcademico())
            .tienePermisoAcademia(Boolean.TRUE.equals(dto.getTienePermisoAcademia()))
            .horaEntradaAcademia(dto.getHoraEntradaAcademia())
            .nombreApoderado(dto.getNombreApoderado())
            .celularApoderado(dto.getCelularApoderado())
            .emailApoderado(dto.getEmailApoderado())
            .estadoMatricula(Alumno.EstadoMatricula.ACTIVO);

        if (dto.getFechaNacimiento() != null) {
            builder.fechaNacimiento(LocalDate.parse(dto.getFechaNacimiento()));
        }
        if (dto.getSexo() != null) {
            builder.sexo(Alumno.Sexo.valueOf(dto.getSexo()));
        }
        if (dto.getAulaId() != null) {
            Aula aula = new Aula();
            aula.setId(dto.getAulaId());
            builder.aula(aula);
        }

        Alumno guardado = alumnoRepository.save(builder.build());
        log.info("Alumno creado: {} (DNI: {})", guardado.getNombreCompleto(), guardado.getDni());

        return AlumnoCreadoResponseDTO.builder()
            .alumno(AlumnoResponseDTO.fromEntity(guardado))
            .usernameGenerado(usuario.getUsername())
            .passwordInicial(CredencialesGenerator.generarPasswordInicial(dto.getDni(), dto.getApellidoPaterno()))
            .build();
    }

    @Override
    @Transactional
    public AlumnoResponseDTO actualizar(Long id, AlumnoRequestDTO dto) {
        Alumno alumno = alumnoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado con ID: " + id));

        alumno.setApellidoPaterno(dto.getApellidoPaterno());
        alumno.setApellidoMaterno(dto.getApellidoMaterno());
        alumno.setNombres(dto.getNombres());
        alumno.setNombreApoderado(dto.getNombreApoderado());
        alumno.setCelularApoderado(dto.getCelularApoderado());
        alumno.setEmailApoderado(dto.getEmailApoderado());
        alumno.setTienePermisoAcademia(Boolean.TRUE.equals(dto.getTienePermisoAcademia()));
        alumno.setHoraEntradaAcademia(dto.getHoraEntradaAcademia());

        if (dto.getTutorId() != null) {
            com.colegio.bastidas.model.Docente tutor = docenteRepository.findById(dto.getTutorId())
                .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado con ID: " + dto.getTutorId()));
            alumno.setTutor(tutor);
        }

        return AlumnoResponseDTO.fromEntity(alumnoRepository.save(alumno));
    }

    @Override
    @Transactional
    public AlumnoResponseDTO actualizarEstado(Long id, String estado) {
        Alumno alumno = alumnoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado con ID: " + id));

        Alumno.EstadoMatricula nuevoEstado;
        try {
            nuevoEstado = Alumno.EstadoMatricula.valueOf(estado);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado inválido: " + estado);
        }

        alumno.setEstadoMatricula(nuevoEstado);
        log.info("Alumno {} cambió su estado de matrícula a {}", alumno.getNombreCompleto(), nuevoEstado);
        return AlumnoResponseDTO.fromEntity(alumnoRepository.save(alumno));
    }

    @Override
    public long contarActivosPorAnio(Integer anio) {
        return alumnoRepository.contarAlumnosActivosPorAnio(anio);
    }

    @Override
    public List<AlumnoResponseDTO> listarConPermisoAcademia(Integer anio) {
        return alumnoRepository
            .findByTienePermisoAcademiaAndAnioAcademico(true, anio)
            .stream()
            .map(AlumnoResponseDTO::fromEntity)
            .toList();
    }
}
