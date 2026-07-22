package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Documento opcional que certifica que el docente tiene o está cursando
 * un curso de docencia. No es obligatorio, pero se destaca en su ficha.
 * A diferencia del expediente del alumno, un docente solo tiene un
 * documento posible — no requiere tipoDocumento ni flujo de verificación.
 */
@Entity
@Table(name = "docente_documentos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DocenteDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    @NotNull
    private Docente docente;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    @NotBlank
    private String nombreArchivo;

    @Column(name = "ruta_storage", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String rutaStorage;

    @Column(name = "url_publica", columnDefinition = "TEXT")
    private String urlPublica;

    @Column(name = "tipo_contenido", length = 100)
    private String tipoContenido;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
