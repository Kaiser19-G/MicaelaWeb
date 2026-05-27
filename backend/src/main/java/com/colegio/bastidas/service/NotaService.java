package com.colegio.bastidas.service;

import com.colegio.bastidas.model.Nota;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Contrato de negocio para el Registro de Notas por Competencias.
 */
public interface NotaService {

    Nota registrarNota(Nota nota);

    Nota actualizarNota(Long notaId, Nota nota);

    List<Nota> obtenerNotasPorAlumnoYAnio(Long alumnoId, Integer anio);

    List<Nota> obtenerNotasPorAulaYArea(Long aulaId, String area, String periodo, Integer anio);

    /**
     * Adjunta una evidencia digital (foto de examen) a una nota.
     *
     * @param notaId  ID de la nota
     * @param archivo Imagen o PDF del examen
     * @param descripcion Descripción de la evidencia
     * @return URL pública de la evidencia en Supabase Storage
     */
    String adjuntarEvidencia(Long notaId, MultipartFile archivo, String descripcion);

    /**
     * Exporta el consolidado de notas en formato Excel compatible con SIAGIE.
     *
     * @param aulaId  ID del aula
     * @param anio    Año académico
     * @return Bytes del archivo Excel
     */
    byte[] exportarConsolidadoNotasSiagie(Long aulaId, Integer anio);

    /** Calcula el promedio final ponderado de un alumno en un período. */
    Double calcularPromedioFinal(Long alumnoId, String periodo, Integer anio);
}
