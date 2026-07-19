package com.colegio.bastidas.dto.reunion;

import com.colegio.bastidas.model.Reunion;
import lombok.Builder;
import lombok.Data;

/**
 * DTO de respuesta de una Reunión. Incluye el celular del apoderado para que
 * el frontend genere el link {@code wa.me} — no se persiste ningún mensaje enviado.
 */
@Data
@Builder
public class ReunionResponseDTO {

    private Long id;
    private Long alumnoId;
    private String nombreAlumno;
    private String nombreApoderado;
    private String celularApoderado;
    private String aulaDescripcion;
    private String fecha;
    private String horaInicio;
    private String horaFin;
    private String motivo;
    private String estado;
    private String convocadaPorUsername;

    public static ReunionResponseDTO fromEntity(Reunion r) {
        return ReunionResponseDTO.builder()
            .id(r.getId())
            .alumnoId(r.getAlumno().getId())
            .nombreAlumno(r.getAlumno().getNombreCompleto())
            .nombreApoderado(r.getAlumno().getNombreApoderado())
            .celularApoderado(r.getAlumno().getCelularApoderado())
            .aulaDescripcion(r.getAula().getDescripcion())
            .fecha(r.getFecha().toString())
            .horaInicio(r.getHoraInicio())
            .horaFin(r.getHoraFin())
            .motivo(r.getMotivo())
            .estado(r.getEstado().name())
            .convocadaPorUsername(r.getConvocadaPor().getUsername())
            .build();
    }
}
