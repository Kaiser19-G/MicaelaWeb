package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.model.ExpedienteDocumento;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.ExpedienteDocumentoRepository;
import com.colegio.bastidas.service.MatriculaService;
import com.colegio.bastidas.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
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

    // ── matricularAlumno ──────────────────────────────────────────────────
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
        alumno.setCodigoEstudiante(generarCodigoEstudiante(alumno.getDni(), anioAcademico));

        Alumno guardado = alumnoRepository.save(alumno);
        log.info("Matrícula exitosa: código={}", guardado.getCodigoEstudiante());
        return guardado;
    }

    // ── cargarDocumentoExpediente ──────────────────────────────────────────
    @Override
    public String cargarDocumentoExpediente(Long alumnoId, MultipartFile archivo,
                                             String tipoDocumento) {
        log.info("Cargando documento: alumno={}, tipo={}", alumnoId, tipoDocumento);

        String extension = FilenameUtils.getExtension(archivo.getOriginalFilename());
        String ruta = String.format("expedientes/%d/%s_%d.%s",
            alumnoId, tipoDocumento.toLowerCase(), System.currentTimeMillis(), extension);

        String urlPublica = storageService.subirArchivo(archivo, ruta);
        log.info("Documento cargado exitosamente: url={}", urlPublica);
        return urlPublica;
    }

    // ── verificarExpedienteCompleto ────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public boolean verificarExpedienteCompleto(Long alumnoId) {
        // TODO: Consultar ExpedienteDocumentoRepository para verificar que existan
        //       al menos DNI y Partida de Nacimiento verificados
        log.debug("Verificando expediente completo para alumno={}", alumnoId);
        return false; // Implementar según regla de negocio
    }

    // ── exportarConsolidadoMatriculaSiagie ─────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public byte[] exportarConsolidadoMatriculaSiagie(Long aulaId, Integer anioAcademico) {
        log.info("Exportando consolidado SIAGIE: aula={}, año={}", aulaId, anioAcademico);

        List<Alumno> alumnos = aulaId != null
            ? alumnoRepository.findByAulaIdAndEstadoMatricula(aulaId, Alumno.EstadoMatricula.ACTIVO)
            : alumnoRepository.findByAnioAcademicoAndEstadoMatricula(anioAcademico, Alumno.EstadoMatricula.ACTIVO);

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Nómina de Matrícula");

            // Estilo de cabecera
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Fila de cabeceras (formato compatible con SIAGIE)
            Row header = sheet.createRow(0);
            String[] columnas = {
                "N°", "CÓDIGO ESTUDIANTE", "DNI", "APELLIDO PATERNO",
                "APELLIDO MATERNO", "NOMBRES", "FECHA NACIMIENTO",
                "SEXO", "AULA", "ESTADO MATRÍCULA", "APODERADO", "CELULAR APODERADO"
            };
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Filas de datos
            int rowNum = 1;
            for (Alumno a : alumnos) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(a.getCodigoEstudiante());
                row.createCell(2).setCellValue(a.getDni());
                row.createCell(3).setCellValue(a.getApellidoPaterno());
                row.createCell(4).setCellValue(a.getApellidoMaterno());
                row.createCell(5).setCellValue(a.getNombres());
                row.createCell(6).setCellValue(
                    a.getFechaNacimiento() != null ? a.getFechaNacimiento().toString() : "");
                row.createCell(7).setCellValue(
                    a.getSexo() != null ? a.getSexo().name() : "");
                row.createCell(8).setCellValue(
                    a.getAula() != null ? a.getAula().getDescripcion() : "");
                row.createCell(9).setCellValue(a.getEstadoMatricula().name());
                row.createCell(10).setCellValue(
                    a.getNombreApoderado() != null ? a.getNombreApoderado() : "");
                row.createCell(11).setCellValue(
                    a.getCelularApoderado() != null ? a.getCelularApoderado() : "");
            }

            wb.write(out);
            log.info("Consolidado SIAGIE generado: {} alumnos", alumnos.size());
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generando consolidado SIAGIE", e);
            throw new RuntimeException("Error al generar el consolidado SIAGIE", e);
        }
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

    // ── Utilidades Privadas ────────────────────────────────────────────────
    private String generarCodigoEstudiante(String dni, Integer anio) {
        return String.format("IE-MB-%d-%s", anio, dni);
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

        com.colegio.bastidas.model.Matricula matricula = com.colegio.bastidas.model.Matricula.builder()
                .alumno(alumno)
                .grado(dto.getGrado())
                .seccion(dto.getSeccion())
                .anioEscolar(dto.getAnioEscolar())
                .estado(dto.getEstado() != null ? dto.getEstado() : com.colegio.bastidas.model.Matricula.EstadoMatricula.ACTIVO)
                .build();

        return mapToDto(matriculaEntityRepository.save(matricula));
    }

    @Override
    public com.colegio.bastidas.dto.MatriculaDto actualizarMatricula(Long id, com.colegio.bastidas.dto.MatriculaDto dto) {
        com.colegio.bastidas.model.Matricula matricula = matriculaEntityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Matrícula no encontrada"));

        matricula.setGrado(dto.getGrado());
        matricula.setSeccion(dto.getSeccion());
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
        dto.setGrado(matricula.getGrado());
        dto.setSeccion(matricula.getSeccion());
        dto.setAnioEscolar(matricula.getAnioEscolar());
        dto.setEstado(matricula.getEstado());
        return dto;
    }
}
