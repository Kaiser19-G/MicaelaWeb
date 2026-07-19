package com.colegio.bastidas.dto.calidad;

import lombok.Builder;
import lombok.Data;

/** Resumen institucional del módulo de Calidad (KPIs + progreso global). */
@Data
@Builder
public class CalidadResumenDTO {

    private int totalAulas;
    private int aulasOk;
    private int conAdvertencias;
    private int conErrores;

    private int estudiantesConNotas;
    private int estudiantesTotal;
}
