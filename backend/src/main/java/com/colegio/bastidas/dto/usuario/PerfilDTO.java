package com.colegio.bastidas.dto.usuario;

import com.colegio.bastidas.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

/**
 * Datos de "Mi Perfil": lo que cada usuario puede ver y editar de sí mismo.
 */
@Data
@Builder
public class PerfilDTO {

    private Long id;
    private String username;
    private String rol;
    private String nombreCompleto;

    @Size(max = 15, message = "El celular no debe exceder 15 caracteres")
    private String celular;

    @Email(message = "El email no tiene formato válido")
    private String email;

    public static PerfilDTO fromEntity(Usuario u) {
        return PerfilDTO.builder()
            .id(u.getId())
            .username(u.getUsername())
            .rol(u.getRol().name())
            .nombreCompleto(u.getNombreCompleto())
            .celular(u.getCelular())
            .email(u.getEmail())
            .build();
    }
}
