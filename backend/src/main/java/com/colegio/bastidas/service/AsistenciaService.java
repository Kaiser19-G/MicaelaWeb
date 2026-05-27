package com.colegio.bastidas.service;

import com.colegio.bastidas.model.Asistencia;
import com.colegio.bastidas.model.Asistencia.EstadoAsistencia;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato de negocio para el Control de Asistencia.
 * Incluye validaciones para el Permiso de Academia y alertas de inasistencia.
 */
public interface AsistenciaService {

    /**
     * Registra la asistencia de un alumno para la fecha indicada.
     * Aplica automáticamente la lógica de permiso de academia si corresponde.
     *
     * @param alumnoId ID del alumno
     * @param fecha    Fecha del registro
     * @param estado   Estado de asistencia observado
     * @param docenteId ID del docente que registra
     * @return Asistencia persistida
     */
    Asistencia registrarAsistencia(Long alumnoId, LocalDate fecha,
                                   EstadoAsistencia estado, Long docenteId);

    /**
     * Registra en lote la asistencia de toda un aula para una fecha.
     * Optimizado para la vista móvil de marcado rápido.
     *
     * @param aulaId    ID del aula
     * @param fecha     Fecha del registro
     * @param registros Lista de pares (alumnoId → estado)
     * @param docenteId Docente que registra
     * @return Lista de asistencias persistidas
     */
    List<Asistencia> registrarAsistenciaLote(Long aulaId, LocalDate fecha,
                                              List<RegistroAsistenciaDto> registros,
                                              Long docenteId);

    /**
     * Verifica si un alumno tiene permiso de academia activo y cuál es
     * su horario de entrada tolerado (13:30 o 14:30).
     *
     * @param alumnoId ID del alumno
     * @return true si tiene permiso de academia activo
     */
    boolean verificarHorarioAcademia(Long alumnoId);

    /**
     * Obtiene la hora de entrada permitida para un alumno con permiso de academia.
     *
     * @param alumnoId ID del alumno
     * @return String con la hora "13:30" o "14:30", o null si no tiene permiso
     */
    String obtenerHoraPermisoAcademia(Long alumnoId);

    /**
     * Procesa y envía alertas de inasistencia al apoderado vía notificación.
     * Se ejecuta automáticamente cuando un alumno acumula N faltas en el período.
     *
     * @param alumnoId   ID del alumno
     * @param fechaInicio Inicio del período de análisis
     * @param fechaFin    Fin del período de análisis
     */
    void procesarAlertaInasistencia(Long alumnoId, LocalDate fechaInicio, LocalDate fechaFin);

    /**
     * Sincroniza registros de asistencia capturados offline en el dispositivo móvil.
     * Aplica deduplicación para evitar duplicados.
     *
     * @param registros Lista de asistencias capturadas offline
     * @return Número de registros sincronizados exitosamente
     */
    int sincronizarRegistrosOffline(List<Asistencia> registros);

    /**
     * Obtiene el resumen de asistencia de un alumno en un rango de fechas.
     *
     * @param alumnoId ID del alumno
     * @param inicio   Fecha de inicio
     * @param fin      Fecha de fin
     * @return Resumen con totales por estado
     */
    ResumenAsistenciaDto obtenerResumenAlumno(Long alumnoId, LocalDate inicio, LocalDate fin);

    /**
     * Obtiene la lista de asistencias del aula para una fecha específica.
     *
     * @param aulaId ID del aula
     * @param fecha  Fecha a consultar
     * @return Lista de asistencias del aula
     */
    List<Asistencia> obtenerAsistenciasAulaPorFecha(Long aulaId, LocalDate fecha);

    /**
     * Detecta alumnos con faltas excesivas en el período para el panel del director.
     *
     * @param aulaId   ID del aula
     * @param inicio   Inicio del período
     * @param fin      Fin del período
     * @param minFaltas Umbral mínimo de faltas para la alerta
     * @return Lista de alumnos con faltas excesivas
     */
    List<AlertaFaltaDto> detectarAlumnosConFaltasExcesivas(Long aulaId,
                                                            LocalDate inicio,
                                                            LocalDate fin,
                                                            int minFaltas);

    // ── DTOs internos del servicio ─────────────────────────────────────────
    record RegistroAsistenciaDto(Long alumnoId, EstadoAsistencia estado, String justificacion) {}

    record ResumenAsistenciaDto(
        int totalDias,
        int asistencias,
        int faltas,
        int tardanzas,
        int justificados,
        int permisosAcademia,
        double porcentajeAsistencia
    ) {}

    record AlertaFaltaDto(Long alumnoId, String nombreAlumno, long totalFaltas) {}
}
