package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.service.ExcelReportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Implementación basada en Apache POI (SXSSFWorkbook: escritura en streaming,
 * evita cargar todo el libro en memoria — importante para exportar ~1500 alumnos).
 */
@Service
@Slf4j
public class ExcelReportServiceImpl implements ExcelReportService {

    @Override
    public byte[] construirLibro(String nombreHoja, String[] cabeceras, List<Object[]> filas) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet(nombreHoja.length() > 31 ? nombreHoja.substring(0, 31) : nombreHoja);

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < cabeceras.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cabeceras[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            int rowNum = 1;
            for (Object[] fila : filas) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < fila.length; i++) {
                    escribirCelda(row.createCell(i), fila[i]);
                }
            }

            wb.write(out);
            wb.dispose(); // limpia los archivos temporales del streaming
            log.info("Excel generado: hoja='{}', filas={}", nombreHoja, filas.size());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Error generando archivo Excel", e);
            throw new RuntimeException("Error al generar el archivo Excel", e);
        }
    }

    private void escribirCelda(Cell cell, Object valor) {
        if (valor == null) {
            cell.setCellValue("");
        } else if (valor instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (valor instanceof Boolean b) {
            cell.setCellValue(b);
        } else {
            cell.setCellValue(valor.toString());
        }
    }
}
