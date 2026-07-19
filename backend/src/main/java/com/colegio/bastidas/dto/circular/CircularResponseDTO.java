package com.colegio.bastidas.dto.circular;

import com.colegio.bastidas.model.Circular;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CircularResponseDTO {

    private Long id;
    private String titulo;
    private String contenido;
    private String dirigidoA;
    private boolean publicada;
    private String fechaPublicacion;
    private String publicadaPorUsername;
    private String createdAt;

    public static CircularResponseDTO fromEntity(Circular c) {
        return CircularResponseDTO.builder()
            .id(c.getId())
            .titulo(c.getTitulo())
            .contenido(c.getContenido())
            .dirigidoA(c.getDirigidoA().name())
            .publicada(Boolean.TRUE.equals(c.getPublicada()))
            .fechaPublicacion(c.getFechaPublicacion() != null ? c.getFechaPublicacion().toString() : null)
            .publicadaPorUsername(c.getPublicadaPor().getUsername())
            .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
            .build();
    }
}
