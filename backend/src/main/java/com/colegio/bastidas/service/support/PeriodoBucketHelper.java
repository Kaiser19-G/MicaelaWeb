package com.colegio.bastidas.service.support;

import com.colegio.bastidas.dto.dashboard.DashboardKpiDTO;
import com.colegio.bastidas.dto.dashboard.PeriodoResumen;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Genera los "buckets" (rangos de fecha + etiqueta) del gráfico de asistencia
 * del Dashboard y del módulo Aulas, y aplica sobre ellos los conteos crudos
 * (fecha, cantidad) obtenidos de una única consulta GROUP BY fecha — evita
 * depender de funciones de fecha específicas de Postgres (date_trunc).
 */
public final class PeriodoBucketHelper {

    private PeriodoBucketHelper() {}

    public record Bucket(String etiqueta, LocalDate inicio, LocalDate fin) {}

    private static final String[] DIAS_SEMANA = {"Lun", "Mar", "Mié", "Jue", "Vie"};
    private static final String[] MESES =
        {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    public static List<Bucket> construirBuckets(PeriodoResumen tipo, LocalDate hoy, Integer anio, Integer mes) {
        return switch (tipo) {
            case DIA -> List.of(new Bucket("Hoy", hoy, hoy));
            case SEMANA -> construirBucketsSemana(hoy);
            case MES -> construirBucketsMes(anio != null ? anio : hoy.getYear(), mes != null ? mes : hoy.getMonthValue());
            case ANIO -> construirBucketsAnio(anio != null ? anio : hoy.getYear());
        };
    }

    private static List<Bucket> construirBucketsSemana(LocalDate hoy) {
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);
        List<Bucket> buckets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            LocalDate dia = lunes.plusDays(i);
            buckets.add(new Bucket(DIAS_SEMANA[i], dia, dia));
        }
        return buckets;
    }

    private static List<Bucket> construirBucketsMes(int anio, int mes) {
        LocalDate inicioMes = LocalDate.of(anio, mes, 1);
        LocalDate finMes = inicioMes.plusMonths(1).minusDays(1);
        List<Bucket> buckets = new ArrayList<>();
        LocalDate cursor = inicioMes;
        int numSemana = 1;
        while (!cursor.isAfter(finMes)) {
            LocalDate finBloque = cursor.plusDays(6);
            if (finBloque.isAfter(finMes)) finBloque = finMes;
            buckets.add(new Bucket("Sem " + numSemana, cursor, finBloque));
            cursor = finBloque.plusDays(1);
            numSemana++;
        }
        return buckets;
    }

    private static List<Bucket> construirBucketsAnio(int anio) {
        List<Bucket> buckets = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            LocalDate inicioMes = LocalDate.of(anio, m, 1);
            LocalDate finMes = inicioMes.plusMonths(1).minusDays(1);
            buckets.add(new Bucket(MESES[m - 1], inicioMes, finMes));
        }
        return buckets;
    }

    /**
     * Suma, para cada bucket, los conteos crudos {@code (fecha, cantidad)} cuya
     * fecha cae dentro de su rango [inicio, fin].
     */
    public static List<DashboardKpiDTO.AsistenciaDiaDTO> aplicarConteos(List<Bucket> buckets, List<Object[]> filasFechaConteo) {
        Map<LocalDate, Long> conteoPorFecha = filasFechaConteo.stream()
            .collect(Collectors.toMap(fila -> (LocalDate) fila[0], fila -> (Long) fila[1]));

        List<DashboardKpiDTO.AsistenciaDiaDTO> resultado = new ArrayList<>();
        for (Bucket bucket : buckets) {
            long total = conteoPorFecha.entrySet().stream()
                .filter(e -> !e.getKey().isBefore(bucket.inicio()) && !e.getKey().isAfter(bucket.fin()))
                .mapToLong(Map.Entry::getValue)
                .sum();
            resultado.add(DashboardKpiDTO.AsistenciaDiaDTO.builder()
                .dia(bucket.etiqueta())
                .alumnos(total)
                .docentes(0L)
                .build());
        }
        return resultado;
    }
}
