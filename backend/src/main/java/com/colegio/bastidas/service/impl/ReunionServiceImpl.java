package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.dto.reunion.ReunionRequestDTO;
import com.colegio.bastidas.dto.reunion.ReunionResponseDTO;
import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.model.Aula;
import com.colegio.bastidas.model.Docente;
import com.colegio.bastidas.model.Reunion;
import com.colegio.bastidas.model.Usuario;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.AulaRepository;
import com.colegio.bastidas.repository.DocenteRepository;
import com.colegio.bastidas.repository.ReunionRepository;
import com.colegio.bastidas.repository.UsuarioRepository;
import com.colegio.bastidas.service.ReunionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReunionServiceImpl implements ReunionService {

    private final ReunionRepository reunionRepository;
    private final AlumnoRepository alumnoRepository;
    private final AulaRepository aulaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;

    @Override
    public ReunionResponseDTO crear(ReunionRequestDTO dto, Authentication authentication) {
        if (dto.getAlumnoId() == null) {
            throw new IllegalArgumentException("Debe indicar el alumno para una reunión individual.");
        }
        Alumno alumno = alumnoRepository.findById(dto.getAlumnoId())
            .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado con ID: " + dto.getAlumnoId()));
        if (alumno.getAula() == null) {
            throw new IllegalArgumentException("El alumno no tiene un aula asignada.");
        }
        if (alumno.getCelularApoderado() == null || alumno.getCelularApoderado().isBlank()) {
            throw new IllegalArgumentException("El apoderado de este alumno no tiene un celular registrado.");
        }

        Usuario convocante = validarPermiso(authentication, alumno.getAula(), alumno);
        Reunion reunion = guardarReunion(dto, alumno, alumno.getAula(), convocante);
        return ReunionResponseDTO.fromEntity(reunion);
    }

    @Override
    public List<ReunionResponseDTO> crearParaAula(Long aulaId, ReunionRequestDTO dto, Authentication authentication) {
        Aula aula = aulaRepository.findById(aulaId)
            .orElseThrow(() -> new IllegalArgumentException("Aula no encontrada con ID: " + aulaId));

        Usuario convocante = validarPermiso(authentication, aula, null);
        Docente docenteConvocante = "DOCENTE".equals(convocante.getRol().name())
            ? docenteRepository.findByUsuarioId(convocante.getId()).orElse(null)
            : null;

        List<Alumno> alumnos = alumnoRepository.findByAulaIdAndEstadoMatricula(aulaId, Alumno.EstadoMatricula.ACTIVO);
        if (docenteConvocante != null) {
            alumnos = alumnos.stream()
                .filter(a -> a.getTutor() != null && a.getTutor().getId().equals(docenteConvocante.getId()))
                .toList();
        }
        alumnos = alumnos.stream()
            .filter(a -> a.getCelularApoderado() != null && !a.getCelularApoderado().isBlank())
            .toList();

        if (alumnos.isEmpty()) {
            throw new IllegalArgumentException(
                "No hay alumnos con celular de apoderado registrado en esta aula (o no es tutor de ninguno).");
        }

        return alumnos.stream()
            .map(alumno -> ReunionResponseDTO.fromEntity(guardarReunion(dto, alumno, aula, convocante)))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReunionResponseDTO> listarProximas() {
        return reunionRepository.findByFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(LocalDate.now())
            .stream().map(ReunionResponseDTO::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReunionResponseDTO> listarPorAula(Long aulaId) {
        return reunionRepository.findByAulaIdOrderByFechaAscHoraInicioAsc(aulaId)
            .stream().map(ReunionResponseDTO::fromEntity).toList();
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private Reunion guardarReunion(ReunionRequestDTO dto, Alumno alumno, Aula aula, Usuario convocante) {
        Reunion reunion = Reunion.builder()
            .alumno(alumno)
            .aula(aula)
            .convocadaPor(convocante)
            .fecha(dto.getFecha())
            .horaInicio(dto.getHoraInicio())
            .horaFin(dto.getHoraFin())
            .motivo(dto.getMotivo())
            .build();
        Reunion guardado = reunionRepository.save(reunion);
        log.info("Reunión agendada: alumno={} aula={} fecha={} por={}",
            alumno.getNombreCompleto(), aula.getDescripcion(), dto.getFecha(), convocante.getUsername());
        return guardado;
    }

    /**
     * DIRECTOR/ADMIN: sin restricciones. DOCENTE: solo puede convocar reuniones
     * en aulas de SECUNDARIA, y solo para alumnos de los que es tutor.
     */
    private Usuario validarPermiso(Authentication authentication, Aula aula, Alumno alumnoIndividual) {
        Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (usuario.getRol() == Usuario.Rol.DIRECTOR || usuario.getRol() == Usuario.Rol.ADMIN) {
            return usuario;
        }

        if (usuario.getRol() != Usuario.Rol.DOCENTE) {
            throw new IllegalArgumentException("No autorizado para agendar reuniones.");
        }

        if (aula.getNivel() != Aula.Nivel.SECUNDARIA) {
            throw new IllegalArgumentException(
                "Solo el Director puede agendar reuniones en aulas de Primaria.");
        }

        Docente docente = docenteRepository.findByUsuarioId(usuario.getId())
            .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado para este usuario"));

        if (alumnoIndividual != null) {
            boolean esTutor = alumnoIndividual.getTutor() != null
                && alumnoIndividual.getTutor().getId().equals(docente.getId());
            if (!esTutor) {
                throw new IllegalArgumentException(
                    "Solo el docente-tutor del aula puede agendar una reunión con este apoderado.");
            }
        }

        return usuario;
    }
}
