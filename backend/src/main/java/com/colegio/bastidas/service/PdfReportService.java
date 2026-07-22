package com.colegio.bastidas.service;

import java.util.List;

/**
 * Construye archivos PDF reutilizables para los distintos reportes del sistema
 * (matrícula SIAGIE, docentes, resumen del dashboard), en el mismo espíritu que
 * {@link ExcelReportService} pero en formato imprimible.
 */
public interface PdfReportService {

    /**
     * Genera un documento PDF tabular (A4 horizontal) a partir de un título, cabeceras y filas.
     *
     * @param titulo    título mostrado en la parte superior del documento
     * @param cabeceras títulos de columna
     * @param filas     cada fila es un arreglo de valores (String, Number, Boolean o null)
     * @return bytes del archivo .pdf generado
     */
    byte[] construirDocumento(String titulo, String[] cabeceras, List<Object[]> filas);
}
