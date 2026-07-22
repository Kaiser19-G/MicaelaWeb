package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.service.PdfReportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementación basada en Apache PDFBox: dibuja un título, una fila de cabecera
 * en negrita y filas de datos de ancho fijo, paginando manualmente cuando el
 * cursor llega al margen inferior (PDFBox no pagina tablas automáticamente).
 */
@Service
@Slf4j
public class PdfReportServiceImpl implements PdfReportService {

    private static final PDRectangle PAGE_SIZE =
        new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()); // A4 horizontal
    private static final float MARGEN = 36f;
    private static final float ALTO_FILA = 16f;
    private static final float TAM_FUENTE_TITULO = 14f;
    private static final float TAM_FUENTE_CABECERA = 9f;
    private static final float TAM_FUENTE_DATO = 8.5f;

    @Override
    public byte[] construirDocumento(String titulo, String[] cabeceras, List<Object[]> filas) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDFont fuenteNegrita = PDType1Font.HELVETICA_BOLD;
            PDFont fuenteNormal = PDType1Font.HELVETICA;

            float anchoUtil = PAGE_SIZE.getWidth() - (2 * MARGEN);
            float[] anchosColumna = new float[cabeceras.length];
            for (int i = 0; i < cabeceras.length; i++) {
                anchosColumna[i] = anchoUtil / cabeceras.length;
            }

            EstadoPagina pagina = nuevaPagina(doc);
            dibujarTitulo(pagina, fuenteNegrita, titulo);
            dibujarCabecera(pagina, fuenteNegrita, cabeceras, anchosColumna);

            for (Object[] fila : filas) {
                if (pagina.y < MARGEN + ALTO_FILA) {
                    pagina.stream.close();
                    pagina = nuevaPagina(doc);
                    dibujarCabecera(pagina, fuenteNegrita, cabeceras, anchosColumna);
                }
                dibujarFila(pagina, fuenteNormal, fila, anchosColumna);
            }
            pagina.stream.close();

            doc.save(out);
            log.info("PDF generado: título='{}', filas={}", titulo, filas.size());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Error generando archivo PDF", e);
            throw new RuntimeException("Error al generar el archivo PDF", e);
        }
    }

    private EstadoPagina nuevaPagina(PDDocument doc) throws IOException {
        PDPage page = new PDPage(PAGE_SIZE);
        doc.addPage(page);
        PDPageContentStream stream = new PDPageContentStream(doc, page);
        EstadoPagina estado = new EstadoPagina();
        estado.stream = stream;
        estado.y = PAGE_SIZE.getHeight() - MARGEN;
        return estado;
    }

    private void dibujarTitulo(EstadoPagina pagina, PDFont fuente, String titulo) throws IOException {
        pagina.stream.beginText();
        pagina.stream.setFont(fuente, TAM_FUENTE_TITULO);
        pagina.stream.newLineAtOffset(MARGEN, pagina.y);
        pagina.stream.showText(titulo);
        pagina.stream.endText();
        pagina.y -= TAM_FUENTE_TITULO;

        String fecha = "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        pagina.stream.beginText();
        pagina.stream.setFont(PDType1Font.HELVETICA_OBLIQUE, 8f);
        pagina.stream.newLineAtOffset(MARGEN, pagina.y - 4);
        pagina.stream.showText(fecha);
        pagina.stream.endText();
        pagina.y -= (ALTO_FILA + 6);
    }

    private void dibujarCabecera(EstadoPagina pagina, PDFont fuenteNegrita, String[] cabeceras, float[] anchos) throws IOException {
        float x = MARGEN;
        for (int i = 0; i < cabeceras.length; i++) {
            escribirCelda(pagina, fuenteNegrita, TAM_FUENTE_CABECERA, x, cabeceras[i], anchos[i]);
            x += anchos[i];
        }
        pagina.y -= ALTO_FILA;
    }

    private void dibujarFila(EstadoPagina pagina, PDFont fuenteNormal, Object[] fila, float[] anchos) throws IOException {
        float x = MARGEN;
        for (int i = 0; i < fila.length && i < anchos.length; i++) {
            escribirCelda(pagina, fuenteNormal, TAM_FUENTE_DATO, x, formatearValor(fila[i]), anchos[i]);
            x += anchos[i];
        }
        pagina.y -= ALTO_FILA;
    }

    private void escribirCelda(EstadoPagina pagina, PDFont fuente, float tamFuente, float x, String texto, float ancho) throws IOException {
        String truncado = truncarParaAncho(fuente, tamFuente, texto == null ? "" : texto, ancho - 4);
        pagina.stream.beginText();
        pagina.stream.setFont(fuente, tamFuente);
        pagina.stream.newLineAtOffset(x + 2, pagina.y);
        pagina.stream.showText(truncado);
        pagina.stream.endText();
    }

    private String truncarParaAncho(PDFont fuente, float tamFuente, String texto, float anchoMax) throws IOException {
        if (anchoDeTexto(fuente, tamFuente, texto) <= anchoMax) {
            return texto;
        }
        String sufijo = "...";
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            String candidato = sb.toString() + c + sufijo;
            if (anchoDeTexto(fuente, tamFuente, candidato) > anchoMax) {
                break;
            }
            sb.append(c);
        }
        return sb + sufijo;
    }

    private float anchoDeTexto(PDFont fuente, float tamFuente, String texto) throws IOException {
        return fuente.getStringWidth(texto) / 1000 * tamFuente;
    }

    private String formatearValor(Object valor) {
        if (valor == null) return "";
        if (valor instanceof Number n) {
            double d = n.doubleValue();
            return (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
        }
        return valor.toString();
    }

    private static class EstadoPagina {
        PDPageContentStream stream;
        float y;
    }
}
