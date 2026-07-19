package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.dto.docente.DocenteCreadoResponseDTO;
import com.colegio.bastidas.dto.docente.DocenteRequestDTO;
import com.colegio.bastidas.dto.docente.DocenteResponseDTO;
import com.colegio.bastidas.model.CursoAsignado;
import com.colegio.bastidas.model.Docente;
import com.colegio.bastidas.model.Usuario;
import com.colegio.bastidas.repository.CursoAsignadoRepository;
import com.colegio.bastidas.repository.DocenteRepository;
import com.colegio.bastidas.repository.EvidenciaRepository;
import com.colegio.bastidas.repository.UsuarioRepository;
import com.colegio.bastidas.service.DocenteService;
import com.colegio.bastidas.service.ExcelReportService;
import com.colegio.bastidas.util.CodigosGenerator;
import com.colegio.bastidas.util.CredencialesGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de Docentes.
 *
 * Semáforo Curricular (RN-04):
 *   APROBADO  → {@value #UMBRAL_APROBADO}+ evidencias registradas.
 *   PENDIENTE → Entre 1 y {@value #UMBRAL_APROBADO}-1 evidencias.
 *   RETRASADO → Ninguna evidencia registrada.
 *
 * NOTA: no existe todavía una entidad de "programación curricular anual /
 * unidades didácticas" en el modelo de datos — se usa la cantidad de
 * Evidencia subida por el docente como proxy de su avance, tal como ya
 * indicaba el comentario original de este archivo ("por ahora se calcula
 * desde cantidadEvidencias"). Los umbrales son ajustables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DocenteServiceImpl implements DocenteService {

    private static final long UMBRAL_APROBADO = 5;

    private final DocenteRepository docenteRepository;
    private final UsuarioProvisioningHelper usuarioProvisioningHelper;
    private final CursoAsignadoRepository cursoAsignadoRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ExcelReportService excelReportService;

    @Override
    public List<DocenteResponseDTO> listarTodos() {
        return docenteRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @Override
    public DocenteResponseDTO buscarPorId(Long id) {
        return docenteRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new RuntimeException("Docente no encontrado con ID: " + id));
    }

    @Override
    public DocenteResponseDTO buscarPorDni(String dni) {
        return docenteRepository.findByDni(dni)
            .map(this::toDTO)
            .orElseThrow(() -> new RuntimeException("Docente no encontrado con DNI: " + dni));
    }

    @Override
    @Transactional
    public DocenteCreadoResponseDTO crear(DocenteRequestDTO dto) {
        if (docenteRepository.existsByDni(dto.getDni())) {
            throw new IllegalArgumentException("Ya existe un docente con DNI: " + dto.getDni());
        }

        Usuario usuario = usuarioProvisioningHelper.crearUsuario(
            dto.getDni(), dto.getApellidoPaterno(), Usuario.Rol.DOCENTE);

        Docente docente = Docente.builder()
            .usuario(usuario)
            .codigoDocente(CodigosGenerator.generarCodigoDocente(dto.getDni()))
            .dni(dto.getDni())
            .apellidoPaterno(dto.getApellidoPaterno())
            .apellidoMaterno(dto.getApellidoMaterno())
            .nombres(dto.getNombres())
            .especialidad(dto.getEspecialidad())
            .condicion(dto.getCondicion() != null
                ? Docente.Condicion.valueOf(dto.getCondicion())
                : Docente.Condicion.NOMBRADO)
            .emailInstitucional(dto.getEmailInstitucional())
            .celular(dto.getCelular())
            .build();
        Docente guardado = docenteRepository.save(docente);
        log.info("Docente creado: {} (DNI: {})", guardado.getNombreCompleto(), guardado.getDni());

        return DocenteCreadoResponseDTO.builder()
            .docente(toDTO(guardado))
            .usernameGenerado(usuario.getUsername())
            .passwordInicial(CredencialesGenerator.generarPasswordInicial(dto.getDni(), dto.getApellidoPaterno()))
            .build();
    }

    @Override
    @Transactional
    public DocenteResponseDTO actualizar(Long id, DocenteRequestDTO dto) {
        Docente docente = docenteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Docente no encontrado con ID: " + id));

        docente.setApellidoPaterno(dto.getApellidoPaterno());
        docente.setApellidoMaterno(dto.getApellidoMaterno());
        docente.setNombres(dto.getNombres());
        docente.setEspecialidad(dto.getEspecialidad());
        if (dto.getCondicion() != null) {
            docente.setCondicion(Docente.Condicion.valueOf(dto.getCondicion()));
        }
        docente.setEmailInstitucional(dto.getEmailInstitucional());
        docente.setCelular(dto.getCelular());

        return toDTO(docenteRepository.save(docente));
    }

    @Override
    @Transactional
    public DocenteResponseDTO actualizarEstadoActivo(Long id, boolean activo) {
        Docente docente = docenteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado con ID: " + id));

        Usuario usuario = docente.getUsuario();
        if (usuario == null) {
            throw new IllegalArgumentException("Este docente no tiene una cuenta de usuario asociada.");
        }
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);

        log.info("Docente {} {}", docente.getNombreCompleto(), activo ? "reactivado" : "dado de baja");
        return toDTO(docente);
    }

    @Override
    public List<DocenteResponseDTO> obtenerSemaforoCurricular() {
        // Todos los docentes con su estado curricular calculado
        return docenteRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @Override
    public long contarPorEstadoCurricular(String estado) {
        return docenteRepository.findAll().stream()
            .filter(d -> calcularEstadoCurricular(evidenciaRepository.countByDocenteId(d.getId())).equalsIgnoreCase(estado))
            .count();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarListaConCursos() {
        String[] columnas = {
            "CÓDIGO", "DNI", "APELLIDO PATERNO", "APELLIDO MATERNO", "NOMBRES",
            "ESPECIALIDAD", "CONDICIÓN", "EMAIL INSTITUCIONAL", "CELULAR", "CURSOS ASIGNADOS"
        };

        List<Object[]> filas = new ArrayList<>();
        for (Docente d : docenteRepository.findAll()) {
            List<CursoAsignado> cursos = cursoAsignadoRepository.findByDocenteId(d.getId());
            String cursosTexto = cursos.stream()
                .map(c -> c.getAreaCurricular() + " – " + c.getAula().getDescripcion())
                .collect(Collectors.joining("; "));

            filas.add(new Object[]{
                d.getCodigoDocente(),
                d.getDni(),
                d.getApellidoPaterno(),
                d.getApellidoMaterno(),
                d.getNombres(),
                d.getEspecialidad(),
                d.getCondicion() != null ? d.getCondicion().name() : "",
                d.getEmailInstitucional(),
                d.getCelular(),
                cursosTexto
            });
        }

        return excelReportService.construirLibro("Docentes", columnas, filas);
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private DocenteResponseDTO toDTO(Docente d) {
        DocenteResponseDTO dto = DocenteResponseDTO.fromEntity(d);
        long evidencias = evidenciaRepository.countByDocenteId(d.getId());
        dto.setCantidadEvidencias((int) evidencias);
        dto.setEstadoCurricular(calcularEstadoCurricular(evidencias));
        dto.setCantidadCursosAsignados(cursoAsignadoRepository.findByDocenteId(d.getId()).size());
        return dto;
    }

    /** Lógica del Semáforo Curricular (RN-04) — ver umbral en {@link #UMBRAL_APROBADO}. */
    private String calcularEstadoCurricular(long evidencias) {
        if (evidencias == 0) return "RETRASADO";
        if (evidencias < UMBRAL_APROBADO) return "PENDIENTE";
        return "APROBADO";
    }
}
