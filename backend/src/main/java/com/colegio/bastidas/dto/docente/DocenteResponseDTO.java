package com.colegio.bastidas.dto.docente;

import com.colegio.bastidas.model.Docente;
import lombok.Builder;
import lombok.Data;

/**
 * DTO de respuesta para Docente.
 * Expone solo los datos necesarios para el frontend (nunca la entidad JPA directamente).
 */
@Data
@Builder
public class DocenteResponseDTO {

    private Long id;
    private String codigoDocente;
    private String dni;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombres;
    private String nombreCompleto;   // "APELLIDOS, Nombres" — listo para mostrar en tabla
    private String especialidad;
    private String condicion;        // "NOMBRADO" | "CONTRATADO"
    private String emailInstitucional;
    private String celular;

    // ── Datos para Semáforo Curricular ──────────────────────────────────────
    /** Estado de programación curricular: APROBADO | PENDIENTE | RETRASADO */
    private String estadoCurricular;
    /** Cantidad de evidencias subidas en el período actual */
    private Integer cantidadEvidencias;

    public static DocenteResponseDTO fromEntity(Docente d) {
        return DocenteResponseDTO.builder()
            .id(d.getId())
            .codigoDocente(d.getCodigoDocente())
            .dni(d.getDni())
            .apellidoPaterno(d.getApellidoPaterno())
            .apellidoMaterno(d.getApellidoMaterno())
            .nombres(d.getNombres())
            .nombreCompleto(d.getNombreCompleto())
            .especialidad(d.getEspecialidad())
            .condicion(d.getCondicion() != null ? d.getCondicion().name() : null)
            .emailInstitucional(d.getEmailInstitucional())
            .celular(d.getCelular())
            // Los campos de semáforo se calculan en el servicio y se inyectan aparte
            .estadoCurricular("PENDIENTE")
            .cantidadEvidencias(0)
            .build();
    }
}
