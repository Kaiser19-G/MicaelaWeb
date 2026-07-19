package com.colegio.bastidas.dto.expediente;

import com.colegio.bastidas.model.ExpedienteDocumento;
import lombok.Builder;
import lombok.Data;

/**
 * DTO de respuesta para un documento del expediente de matrícula de un alumno.
 */
@Data
@Builder
public class ExpedienteDocumentoResponseDTO {

    private Long id;
    private String tipoDocumento;
    private String nombreArchivo;
    private String urlPublica;
    private String estadoVerificacion;
    private String createdAt;

    public static ExpedienteDocumentoResponseDTO fromEntity(ExpedienteDocumento d) {
        return ExpedienteDocumentoResponseDTO.builder()
            .id(d.getId())
            .tipoDocumento(d.getTipoDocumento().name())
            .nombreArchivo(d.getNombreArchivo())
            .urlPublica(d.getUrlPublica())
            .estadoVerificacion(d.getEstadoVerificacion() != null ? d.getEstadoVerificacion().name() : null)
            .createdAt(d.getCreatedAt() != null ? d.getCreatedAt().toString() : null)
            .build();
    }
}
