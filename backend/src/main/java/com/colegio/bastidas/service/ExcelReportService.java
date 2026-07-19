package com.colegio.bastidas.service;

import java.util.List;

/**
 * Construye archivos Excel (.xlsx) reutilizables para los distintos reportes
 * del sistema (matrícula SIAGIE, docentes, resumen del dashboard), evitando
 * duplicar el código de Apache POI en cada servicio.
 */
public interface ExcelReportService {

    /**
     * Genera un libro de Excel de una sola hoja a partir de cabeceras y filas.
     *
     * @param nombreHoja nombre de la pestaña (máx. 31 caracteres, límite de Excel)
     * @param cabeceras  títulos de columna
     * @param filas      cada fila es un arreglo de valores (String, Number, Boolean o null)
     * @return bytes del archivo .xlsx generado
     */
    byte[] construirLibro(String nombreHoja, String[] cabeceras, List<Object[]> filas);
}
