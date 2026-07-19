package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.reunion.ReunionRequestDTO;
import com.colegio.bastidas.dto.reunion.ReunionResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Agenda de reuniones con apoderados. El envío del mensaje es manual (link
 * {@code wa.me} generado en el frontend); este servicio solo agenda y valida
 * quién puede convocar a quién.
 */
public interface ReunionService {

    /** Reunión individual con el apoderado de un alumno. */
    ReunionResponseDTO crear(ReunionRequestDTO dto, Authentication authentication);

    /**
     * Reunión general: una convocatoria por cada alumno activo del aula
     * (comparten fecha/hora/motivo). Si quien convoca es DOCENTE, solo se
     * incluyen los alumnos de los que es tutor.
     */
    List<ReunionResponseDTO> crearParaAula(Long aulaId, ReunionRequestDTO dto, Authentication authentication);

    List<ReunionResponseDTO> listarProximas();

    List<ReunionResponseDTO> listarPorAula(Long aulaId);
}
