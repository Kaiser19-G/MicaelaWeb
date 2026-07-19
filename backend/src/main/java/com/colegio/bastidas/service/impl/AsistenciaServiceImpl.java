package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.model.Asistencia;
import com.colegio.bastidas.model.Asistencia.EstadoAsistencia;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.AsistenciaRepository;
import com.colegio.bastidas.repository.DocenteRepository;
import com.colegio.bastidas.service.AsistenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de Asistencia.
 *
 * <p>Lógica de Permiso de Academia:
 * Los alumnos inscritos en academias pre-universitarias pueden ingresar
 * a las 13:30 o 14:30 hs. sin computarse como TARDANZA.
 *
 * <p>Persistencia Offline:
 * Los registros marcados con {@code sincronizadoOffline=true} son ingestados
 * con deduplicación automática por (alumnoId + fecha).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AsistenciaServiceImpl implements AsistenciaService {

    // Umbral de faltas para disparar alerta (configurable vía application.properties)
    private static final int UMBRAL_FALTAS_ALERTA = 3;

    private final AsistenciaRepository asistenciaRepository;
    private final AlumnoRepository alumnoRepository;
    private final DocenteRepository docenteRepository;
    private final AsistenciaRegistroHelper registroHelper;

    // ── registrarAsistencia ────────────────────────────────────────────────
    @Override
    public Asistencia registrarAsistencia(Long alumnoId, LocalDate fecha,
                                          EstadoAsistencia estado, Long docenteId) {
        log.debug("Registrando asistencia: alumno={}, fecha={}, estado={}", alumnoId, fecha, estado);

        // Prevenir duplicados
        if (asistenciaRepository.existsByAlumnoIdAndFecha(alumnoId, fecha)) {
            log.warn("Asistencia ya registrada para alumno={} fecha={}", alumnoId, fecha);
            return asistenciaRepository.findByAlumnoIdAndFecha(alumnoId, fecha).orElseThrow();
        }

        Alumno alumno = alumnoRepository.findById(alumnoId)
            .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado: " + alumnoId));

        // Aplicar lógica de permiso de academia
        boolean aplicarPermiso = false;
        String horaPermiso = null;
        if (Boolean.TRUE.equals(alumno.getTienePermisoAcademia())
                && estado == EstadoAsistencia.TARDANZA) {
            aplicarPermiso = true;
            horaPermiso = alumno.getHoraEntradaAcademia();
            estado = EstadoAsistencia.PERMISO_ACADEMIA;
            log.info("Permiso de academia aplicado para alumno={} hora={}", alumnoId, horaPermiso);
        }

        Asistencia asistencia = Asistencia.builder()
            .alumno(alumno)
            .fecha(fecha)
            .estado(estado)
            .docente(docenteRepository.findById(docenteId).orElse(null))
            .aplicadoPermisoAcademia(aplicarPermiso)
            .horaPermisoAcademia(horaPermiso)
            .build();

        Asistencia guardada = asistenciaRepository.save(asistencia);

        // Disparar alerta asíncrona si corresponde
        if (estado == EstadoAsistencia.FALTA) {
            procesarAlertaInasistencia(alumnoId,
                fecha.withDayOfMonth(1), fecha.withDayOfMonth(fecha.lengthOfMonth()));
        }

        return guardada;
    }

    // ── registrarAsistenciaLote ────────────────────────────────────────────
    @Override
    public List<Asistencia> registrarAsistenciaLote(Long aulaId, LocalDate fecha,
                                                     List<RegistroAsistenciaDto> registros,
                                                     Long docenteId) {
        log.info("Registro masivo de asistencia: aula={}, fecha={}, registros={}",
            aulaId, fecha, registros.size());

        List<Asistencia> resultado = new ArrayList<>();
        for (RegistroAsistenciaDto dto : registros) {
            try {
                // Cada alumno se registra en su propia transacción REQUIRES_NEW
                Asistencia a = registroHelper.registrarUno(
                    dto.alumnoId(), fecha, dto.estado(), docenteId, dto.justificacion());
                resultado.add(a);
                log.debug("Asistencia registrada: alumno={}, estado={}", dto.alumnoId(), dto.estado());
            } catch (Exception ex) {
                log.error("Error al registrar asistencia para alumno={}: {}",
                    dto.alumnoId(), ex.getMessage());
                // continuar con el siguiente alumno
            }
        }
        log.info("Lote completado: {}/{} registros exitosos", resultado.size(), registros.size());
        return resultado;
    }

    // ── verificarHorarioAcademia ───────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public boolean verificarHorarioAcademia(Long alumnoId) {
        return alumnoRepository.findById(alumnoId)
            .map(a -> Boolean.TRUE.equals(a.getTienePermisoAcademia()))
            .orElse(false);
    }

    // ── obtenerHoraPermisoAcademia ─────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public String obtenerHoraPermisoAcademia(Long alumnoId) {
        return alumnoRepository.findById(alumnoId)
            .filter(a -> Boolean.TRUE.equals(a.getTienePermisoAcademia()))
            .map(Alumno::getHoraEntradaAcademia)
            .orElse(null);
    }

    // ── procesarAlertaInasistencia ─────────────────────────────────────────
    @Override
    @Async
    public void procesarAlertaInasistencia(Long alumnoId, LocalDate inicio, LocalDate fin) {
        long totalFaltas = asistenciaRepository.countByAlumnoIdAndEstadoAndFechaBetween(
            alumnoId, EstadoAsistencia.FALTA, inicio, fin);

        if (totalFaltas >= UMBRAL_FALTAS_ALERTA) {
            alumnoRepository.findById(alumnoId).ifPresent(alumno -> {
                log.warn("⚠ ALERTA: Alumno {} tiene {} faltas en el período {}-{}. " +
                    "Notificando al apoderado: {}",
                    alumno.getNombreCompleto(), totalFaltas, inicio, fin,
                    alumno.getCelularApoderado());
                // TODO: Integrar con servicio de notificaciones push / SMS / email
            });
        }
    }

    // ── sincronizarRegistrosOffline ────────────────────────────────────────
    @Override
    public int sincronizarRegistrosOffline(List<Asistencia> registros) {
        log.info("Sincronizando {} registros offline", registros.size());
        int sincronizados = 0;
        for (Asistencia a : registros) {
            if (!asistenciaRepository.existsByAlumnoIdAndFecha(
                    a.getAlumno().getId(), a.getFecha())) {
                a.setSincronizadoOffline(true);
                asistenciaRepository.save(a);
                sincronizados++;
            } else {
                log.debug("Registro duplicado omitido: alumno={} fecha={}",
                    a.getAlumno().getId(), a.getFecha());
            }
        }
        log.info("Sincronización completada: {}/{} registros procesados", sincronizados, registros.size());
        return sincronizados;
    }

    // ── obtenerResumenAlumno ───────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResumenAsistenciaDto obtenerResumenAlumno(Long alumnoId,
                                                      LocalDate inicio, LocalDate fin) {
        List<Asistencia> lista = asistenciaRepository
            .findByAlumnoIdAndFechaBetween(alumnoId, inicio, fin);

        int asistencias     = (int) lista.stream().filter(a -> a.getEstado() == EstadoAsistencia.ASISTIO).count();
        int faltas          = (int) lista.stream().filter(a -> a.getEstado() == EstadoAsistencia.FALTA).count();
        int tardanzas       = (int) lista.stream().filter(a -> a.getEstado() == EstadoAsistencia.TARDANZA).count();
        int justificados    = (int) lista.stream().filter(a -> a.getEstado() == EstadoAsistencia.JUSTIFICADO).count();
        int permisosAcademia= (int) lista.stream().filter(a -> a.getEstado() == EstadoAsistencia.PERMISO_ACADEMIA).count();
        int total           = lista.size();
        double porcentaje   = total == 0 ? 0.0 : (double) asistencias / total * 100;

        return new ResumenAsistenciaDto(total, asistencias, faltas, tardanzas,
            justificados, permisosAcademia, Math.round(porcentaje * 10.0) / 10.0);
    }

    // ── obtenerAsistenciasAulaPorFecha ─────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<Asistencia> obtenerAsistenciasAulaPorFecha(Long aulaId, LocalDate fecha) {
        return asistenciaRepository
            .findByAulaIdAndFechaOrderByAlumnoApellidoPaterno(aulaId, fecha);
    }

    // ── detectarAlumnosConFaltasExcesivas ──────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<AlertaFaltaDto> detectarAlumnosConFaltasExcesivas(Long aulaId,
                                                                   LocalDate inicio,
                                                                   LocalDate fin,
                                                                   int minFaltas) {
        List<Object[]> raw = asistenciaRepository
            .alumnosConFaltasExcesivas(aulaId, inicio, fin, minFaltas);
        return mapAlertaFaltas(raw);
    }

    // ── detectarAlumnosConFaltasExcesivasGlobal ────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<AlertaFaltaDto> detectarAlumnosConFaltasExcesivasGlobal(LocalDate inicio,
                                                                         LocalDate fin,
                                                                         int minFaltas) {
        List<Object[]> raw = asistenciaRepository
            .alumnosConFaltasExcesivasGlobal(inicio, fin, minFaltas);
        return mapAlertaFaltas(raw);
    }

    private List<AlertaFaltaDto> mapAlertaFaltas(List<Object[]> raw) {
        return raw.stream()
            .map(row -> {
                Long alumnoId = (Long) row[0];
                long totalFaltas = (long) row[1];
                String nombre = alumnoRepository.findById(alumnoId)
                    .map(Alumno::getNombreCompleto)
                    .orElse("Desconocido");
                return new AlertaFaltaDto(alumnoId, nombre, totalFaltas);
            })
            .collect(Collectors.toList());
    }
}
