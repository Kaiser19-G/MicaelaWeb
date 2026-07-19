package com.colegio.bastidas.util;

/**
 * Genera los códigos institucionales (código de estudiante, código de docente)
 * que antes se le pedían al Director escribir a mano al registrar un alumno
 * o docente nuevo.
 */
public final class CodigosGenerator {

    private CodigosGenerator() {
    }

    /**
     * Ej: dni="71512635", anio=2026 -&gt; "IE2671512635".
     * La columna codigo_estudiante es varchar(12): "IE" + 2 dígitos de año + DNI (8) = 12.
     */
    public static String generarCodigoEstudiante(String dni, Integer anio) {
        return String.format("IE%02d%s", anio % 100, dni);
    }

    /**
     * Ej: dni="71512635" -&gt; "DOC71512635".
     * La columna codigo_docente es varchar(12): "DOC" + DNI (8) = 11.
     */
    public static String generarCodigoDocente(String dni) {
        return "DOC" + dni;
    }
}
