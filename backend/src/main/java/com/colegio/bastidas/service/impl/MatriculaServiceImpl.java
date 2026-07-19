package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.exception.AulaCompletaException;
import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.model.Aula;
import com.colegio.bastidas.model.ExpedienteDocumento;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.AulaRepository;
import com.colegio.bastidas.repository.ExpedienteDocumentoRepository;
import com.colegio.bastidas.service.AulaService;
import com.colegio.bastidas.service.ExcelReportService;
import com.colegio.bastidas.service.MatriculaService;
import com.colegio.bastidas.service.SupabaseStorageService;
import com.colegio.bastidas.util.CodigosGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de Matrícula Digital.
 * Gestiona el flujo completo de matrícula y generación de consolidado SIAGIE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MatriculaServiceImpl implements MatriculaService {

    private final AlumnoRepository alumnoRepository;
    private final SupabaseStorageService storageService;
    private final com.colegio.bastidas.repository.MatriculaRepository matriculaEntityRepository;
    private final AulaRepository aulaRepository;
    private final AulaService aulaService;
    private final UsuarioProvisioningHelper usuarioProvisioningHelper;
    private final ExpedienteDocumentoRepository expedienteDocumentoRepository;
    private final ExcelReportService excelReportService;

    // ── matricularAlumno (legado) ──────────────────────────────────────────
    /**
     * @deprecated Flujo legado que no vincula un {@link Aula} ni verifica
     * vacantes. El flujo vigente es {@link #crearMatricula} (usado por
     * {@code MatriculaCrudController} / {@code /api/matriculas-crud}).
     */
    @Deprecated
    @Override
    public Alumno matricularAlumno(Alumno alumno, Integer anioAcademico) {
        log.info("Procesando matrícula: DNI={}, año={}", alumno.getDni(), anioAcademico);

        if (alumnoRepository.existsByDni(alumno.getDni())) {
            // Actualizar matrícula existente
            return alumnoRepository.findByDni(alumno.getDni())
                .map(existente -> {
                    existente.setEstadoMatricula(Alumno.EstadoMatricula.ACTIVO);
                    existente.setAnioAcademico(anioAcademico);
                    return alumnoRepository.save(existente);
                })
                .orElseThrow();
        }

        alumno.setAnioAcademico(anioAcademico);
        alumno.setEstadoMatricula(Alumno.EstadoMatricula.ACTIVO);
        alumno.setCodigoEstudiante(CodigosGenerator.generarCodigoEstudiante(alumno.getDni(), anioAcademico));

        Alumno guardado = alumnoRepository.save(alumno);
        log.info("Matrícula exitosa: código={}", guardado.getCodigoEstudiante());
        return guardado;
    }

    // ── cargarDocumentoExpediente ──────────────────────────────────────────
    @Override
    public String cargarDocumentoExpediente(Long alumnoId, MultipartFile archivo,
                                             String tipoDocumento) {
        log.info("Cargando documento: alumno={}, tipo={}", alumnoId, tipoDocumento);

        if (!"application/pdf".equalsIgnoreCase(archivo.getContentType())) {
            throw new IllegalArgumentException("Solo se admiten archivos en formato PDF.");
        }

        Alumno alumno = alumnoRepository.findById(alumnoId)
            .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado con ID: " + alumnoId));

        ExpedienteDocumento.TipoDocumento tipo;
        try {
            tipo = ExpedienteDocumento.TipoDocumento.valueOf(tipoDocumento);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de documento inválido: " + tipoDocumento);
        }

        String extension = FilenameUtils.getExtension(archivo.getOriginalFilename());
        String ruta = String.format("expedientes/%d/%s_%d.%s",
            alumnoId, tipoDocumento.toLowerCase(), System.currentTimeMillis(), extension);

        String urlPublica = storageService.subirArchivo(archivo, ruta);

        ExpedienteDocumento documento = expedienteDocumentoRepository
            .findFirstByAlumnoIdAndTipoDocumento(alumnoId, tipo)
            .orElseGet(() -> ExpedienteDocumento.builder()
                .alumno(alumno)
                .tipoDocumento(tipo)
                .build());

        documento.setNombreArchivo(archivo.getOriginalFilename());
        documento.setRutaStorage(ruta);
        documento.setUrlPublica(urlPublica);
        documento.setTipoContenido(archivo.getContentType());
        documento.setTamanoBytes(archivo.getSize());
        documento.setEstadoVerificacion(ExpedienteDocumento.EstadoVerificacion.VERIFICADO);
        documento.setAnioMatricula(alumno.getAnioAcademico());
        expedienteDocumentoRepository.save(documento);

        log.info("Documento cargado exitosamente: alumno={}, tipo={}, url={}", alumnoId, tipo, urlPublica);
        return urlPublica;
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.colegio.bastidas.dto.expediente.ExpedienteDocumentoResponseDTO> listarDocumentosExpediente(Long alumnoId) {
        return expedienteDocumentoRepository.findByAlumnoId(alumnoId).stream()
            .map(com.colegio.bastidas.dto.expediente.ExpedienteDocumentoResponseDTO::fromEntity)
            .toList();
    }

    @Override
    public void eliminarDocumentoExpediente(Long documentoId) {
        ExpedienteDocumento documento = expedienteDocumentoRepository.findById(documentoId)
            .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado con ID: " + documentoId));
        storageService.eliminarArchivo(documento.getRutaStorage());
        expedienteDocumentoRepository.delete(documento);
        log.info("Documento de expediente eliminado: id={}", documentoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.colegio.bastidas.dto.expediente.ExpedienteResumenDTO> listarResumenExpedientes(Integer anio) {
        List<Alumno> alumnos = alumnoRepository.findByAnioAcademicoAndEstadoMatricula(anio, Alumno.EstadoMatricula.ACTIVO);
        List<Long> ids = alumnos.stream().map(Alumno::getId).toList();
        List<ExpedienteDocumento> documentos = ids.isEmpty()
            ? List.of()
            : expedienteDocumentoRepository.findByAlumnoIdIn(ids);

        var docsPorAlumno = documentos.stream()
            .collect(java.util.stream.Collectors.groupingBy(d -> d.getAlumno().getId()));

        return alumnos.stream().map(a -> {
            List<ExpedienteDocumento> propios = docsPorAlumno.getOrDefault(a.getId(), List.of());
            boolean tieneDni = propios.stream().anyMatch(d -> d.getTipoDocumento() == ExpedienteDocumento.TipoDocumento.DNI);
            boolean tienePartida = propios.stream().anyMatch(d -> d.getTipoDocumento() == ExpedienteDocumento.TipoDocumento.PARTIDA_NACIMIENTO);
            return com.colegio.bastidas.dto.expediente.ExpedienteResumenDTO.builder()
                .alumnoId(a.getId())
                .nombreCompleto(a.getNombreCompleto())
                .dni(a.getDni())
                .aulaDescripcion(a.getAula() != null ? a.getAula().getDescripcion() : null)
                .tieneDni(tieneDni)
                .tienePartidaNacimiento(tienePartida)
                .totalDocumentos(propios.size())
                .build();
        }).toList();
    }

    // ── verificarExpedienteCompleto ────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public boolean verificarExpedienteCompleto(Long alumnoId) {
        log.debug("Verificando expediente completo para alumno={}", alumnoId);
        boolean tieneDni = expedienteDocumentoRepository.existsByAlumnoIdAndTipoDocumentoAndEstadoVerificacion(
            alumnoId, ExpedienteDocumento.TipoDocumento.DNI, ExpedienteDocumento.EstadoVerificacion.VERIFICADO);
        boolean tienePartida = expedienteDocumentoRepository.existsByAlumnoIdAndTipoDocumentoAndEstadoVerificacion(
            alumnoId, ExpedienteDocumento.TipoDocumento.PARTIDA_NACIMIENTO, ExpedienteDocumento.EstadoVerificacion.VERIFICADO);
        return tieneDni && tienePartida;
    }

    @Override
    @Transactional(readOnly = true)
    public long contarConExpedienteIncompleto(Integer anio) {
        long activos = alumnoRepository.contarAlumnosActivosPorAnio(anio);
        long completos = alumnoRepository.contarConExpedienteCompletoPorAnio(anio);
        return Math.max(0, activos - completos);
    }

    // ── exportarConsolidadoMatriculaSiagie ─────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public byte[] exportarConsolidadoMatriculaSiagie(Long aulaId, Integer anioAcademico) {
        log.info("Exportando consolidado SIAGIE: aula={}, año={}", aulaId, anioAcademico);

        List<Alumno> alumnos = aulaId != null
            ? alumnoRepository.findByAulaIdAndEstadoMatricula(aulaId, Alumno.EstadoMatricula.ACTIVO)
            : alumnoRepository.findByAnioAcademicoAndEstadoMatricula(anioAcademico, Alumno.EstadoMatricula.ACTIVO);

        String[] columnas = {
            "N°", "CÓDIGO ESTUDIANTE", "DNI", "APELLIDO PATERNO",
            "APELLIDO MATERNO", "NOMBRES", "FECHA NACIMIENTO",
            "SEXO", "AULA", "ESTADO MATRÍCULA", "APODERADO", "CELULAR APODERADO"
        };

        List<Object[]> filas = new java.util.ArrayList<>();
        int n = 1;
        for (Alumno a : alumnos) {
            filas.add(new Object[]{
                n++,
                a.getCodigoEstudiante(),
                a.getDni(),
                a.getApellidoPaterno(),
                a.getApellidoMaterno(),
                a.getNombres(),
                a.getFechaNacimiento() != null ? a.getFechaNacimiento().toString() : "",
                a.getSexo() != null ? a.getSexo().name() : "",
                a.getAula() != null ? a.getAula().getDescripcion() : "",
                a.getEstadoMatricula().name(),
                a.getNombreApoderado() != null ? a.getNombreApoderado() : "",
                a.getCelularApoderado() != null ? a.getCelularApoderado() : ""
            });
        }

        byte[] excel = excelReportService.construirLibro("Nómina de Matrícula", columnas, filas);
        log.info("Consolidado SIAGIE generado: {} alumnos", alumnos.size());
        return excel;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Alumno> buscarPorDni(String dni) {
        return alumnoRepository.findByDni(dni);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alumno> buscarPorNombreODni(String termino) {
        return alumnoRepository.buscarPorNombreODni(termino);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alumno> listarPorAula(Long aulaId, Integer anioAcademico) {
        return alumnoRepository.findByAulaIdAndEstadoMatricula(aulaId, Alumno.EstadoMatricula.ACTIVO);
    }

    @Override
    public Alumno actualizarPermisoAcademia(Long alumnoId, boolean tienePermiso, String horaEntrada) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
            .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));
        alumno.setTienePermisoAcademia(tienePermiso);
        alumno.setHoraEntradaAcademia(horaEntrada);
        return alumnoRepository.save(alumno);
    }

    // ── CRUD de la entidad Matricula (Sprint 5) ─────────────────────────────

    @Override
    public List<com.colegio.bastidas.dto.MatriculaDto> listarPorAnio(Integer anio) {
        return matriculaEntityRepository.findByAnioEscolar(anio).stream()
                .map(this::mapToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public com.colegio.bastidas.dto.MatriculaDto crearMatricula(com.colegio.bastidas.dto.MatriculaDto dto) {
        if (matriculaEntityRepository.existsByAlumnoIdAndAnioEscolar(dto.getAlumnoId(), dto.getAnioEscolar())) {
            throw new IllegalArgumentException("El alumno ya está matriculado en el año " + dto.getAnioEscolar());
        }

        Alumno alumno = alumnoRepository.findById(dto.getAlumnoId())
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado con ID: " + dto.getAlumnoId()));

        Aula aula = aulaRepository.findById(dto.getAulaId())
                .orElseThrow(() -> new IllegalArgumentException("Aula no encontrada con ID: " + dto.getAulaId()));

        if (aulaService.contarVacantes(aula.getId()) <= 0) {
            throw new AulaCompletaException(
                "El aula " + aula.getDescripcion() + " no tiene vacantes disponibles.");
        }

        // Aprovisionar cuenta de usuario si el alumno todavía no tiene una
        // (caso normal: se crea al registrar el Alumno vía POST /alumnos).
        if (alumno.getUsuario() == null) {
            var usuario = usuarioProvisioningHelper.crearUsuario(
                alumno.getDni(), alumno.getApellidoPaterno(), com.colegio.bastidas.model.Usuario.Rol.ALUMNO);
            alumno.setUsuario(usuario);
        }

        // Sincronizar el aula "actual" del alumno (varias pantallas la leen
        // directamente desde Alumno, no desde Matricula).
        alumno.setAula(aula);
        alumno.setAnioAcademico(dto.getAnioEscolar());
        alumno.setEstadoMatricula(Alumno.EstadoMatricula.ACTIVO);
        alumnoRepository.save(alumno);

        com.colegio.bastidas.model.Matricula matricula = com.colegio.bastidas.model.Matricula.builder()
                .alumno(alumno)
                .aula(aula)
                .grado(aula.getGrado())
                .seccion(aula.getSeccion())
                .anioEscolar(dto.getAnioEscolar())
                .estado(dto.getEstado() != null ? dto.getEstado() : com.colegio.bastidas.model.Matricula.EstadoMatricula.ACTIVO)
                .build();

        return mapToDto(matriculaEntityRepository.save(matricula));
    }

    @Override
    public com.colegio.bastidas.dto.MatriculaDto actualizarMatricula(Long id, com.colegio.bastidas.dto.MatriculaDto dto) {
        com.colegio.bastidas.model.Matricula matricula = matriculaEntityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Matrícula no encontrada"));

        if (dto.getAulaId() != null && !dto.getAulaId().equals(
                matricula.getAula() != null ? matricula.getAula().getId() : null)) {
            Aula nuevaAula = aulaRepository.findById(dto.getAulaId())
                    .orElseThrow(() -> new IllegalArgumentException("Aula no encontrada con ID: " + dto.getAulaId()));
            if (aulaService.contarVacantes(nuevaAula.getId()) <= 0) {
                throw new AulaCompletaException(
                    "El aula " + nuevaAula.getDescripcion() + " no tiene vacantes disponibles.");
            }
            matricula.setAula(nuevaAula);
            matricula.setGrado(nuevaAula.getGrado());
            matricula.setSeccion(nuevaAula.getSeccion());
            matricula.getAlumno().setAula(nuevaAula);
            alumnoRepository.save(matricula.getAlumno());
        }
        if (dto.getEstado() != null) {
            matricula.setEstado(dto.getEstado());
        }

        return mapToDto(matriculaEntityRepository.save(matricula));
    }

    @Override
    public void eliminarMatricula(Long id) {
        matriculaEntityRepository.deleteById(id);
    }

    private com.colegio.bastidas.dto.MatriculaDto mapToDto(com.colegio.bastidas.model.Matricula matricula) {
        com.colegio.bastidas.dto.MatriculaDto dto = new com.colegio.bastidas.dto.MatriculaDto();
        dto.setId(matricula.getId());
        dto.setAlumnoId(matricula.getAlumno().getId());
        dto.setNombreAlumno(matricula.getAlumno().getNombres() + " " + matricula.getAlumno().getApellidoPaterno() + " " + matricula.getAlumno().getApellidoMaterno());
        dto.setCodigoAlumno(matricula.getAlumno().getCodigoEstudiante());
        dto.setAulaId(matricula.getAula() != null ? matricula.getAula().getId() : null);
        dto.setGrado(matricula.getGrado());
        dto.setSeccion(matricula.getSeccion());
        dto.setAnioEscolar(matricula.getAnioEscolar());
        dto.setEstado(matricula.getEstado());
        return dto;
    }
}
