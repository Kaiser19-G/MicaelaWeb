package com.colegio.bastidas.dto.docente;

import com.colegio.bastidas.model.DocenteDocumento;
import lombok.Builder;
import lombok.Data;

/**
 * DTO de respuesta para el documento de certificación docente (opcional).
 */
@Data
@Builder
public class DocenteDocumentoResponseDTO {

    private Long id;
    private String nombreArchivo;
    private String urlPublica;
    private String createdAt;

    public static DocenteDocumentoResponseDTO fromEntity(DocenteDocumento d) {
        return DocenteDocumentoResponseDTO.builder()
            .id(d.getId())
            .nombreArchivo(d.getNombreArchivo())
            .urlPublica(d.getUrlPublica())
            .createdAt(d.getCreatedAt() != null ? d.getCreatedAt().toString() : null)
            .build();
    }
}
