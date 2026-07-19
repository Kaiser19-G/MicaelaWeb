package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.calidad.AulaValidacionDTO;
import com.colegio.bastidas.dto.calidad.CalidadResumenDTO;

import java.util.List;

/**
 * Validador de Pre-Actas: detecta notas en blanco e inconsistencias por aula
 * antes de la exportación oficial al SIAGIE.
 */
public interface CalidadService {

    List<AulaValidacionDTO> listarValidacionAulas(Integer anio);

    CalidadResumenDTO obtenerResumen(Integer anio);
}
