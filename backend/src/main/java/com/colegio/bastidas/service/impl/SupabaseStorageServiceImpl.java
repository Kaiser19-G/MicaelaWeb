package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementación del servicio de Supabase Storage.
 * Usa la API REST de Supabase Storage para subir, eliminar y firmar archivos.
 *
 * Documentación: https://supabase.com/docs/guides/storage/api
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageServiceImpl implements SupabaseStorageService {

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    @Value("${app.supabase.service-key}")
    private String supabaseServiceKey;

    @Value("${app.supabase.bucket-evidencias}")
    private String bucketEvidencias;

    private final RestTemplate restTemplate;

    @Override
    public String subirArchivo(MultipartFile archivo, String ruta) {
        log.debug("Subiendo archivo a Supabase Storage: {}", ruta);

        String uploadUrl = String.format("%s/storage/v1/object/%s/%s",
            supabaseUrl, bucketEvidencias, ruta);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseServiceKey);
        headers.set("x-upsert", "true"); // Permite sobreescribir si ya existe
        headers.setContentType(MediaType.parseMediaType(
            StringUtils.defaultIfBlank(archivo.getContentType(), "application/octet-stream")));

        try {
            HttpEntity<byte[]> request = new HttpEntity<>(archivo.getBytes(), headers);
            restTemplate.exchange(uploadUrl, HttpMethod.POST, request, String.class);

            // Construir la URL pública
            String urlPublica = String.format("%s/storage/v1/object/public/%s/%s",
                supabaseUrl, bucketEvidencias, ruta);

            log.info("Archivo subido exitosamente: {}", urlPublica);
            return urlPublica;

        } catch (Exception e) {
            log.error("Error al subir archivo a Supabase Storage: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir el archivo al almacenamiento", e);
        }
    }

    @Override
    public void eliminarArchivo(String ruta) {
        log.debug("Eliminando archivo de Supabase Storage: {}", ruta);

        String deleteUrl = String.format("%s/storage/v1/object/%s/%s",
            supabaseUrl, bucketEvidencias, ruta);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseServiceKey);

        try {
            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, request, String.class);
            log.info("Archivo eliminado: {}", ruta);
        } catch (Exception e) {
            log.error("Error al eliminar archivo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al eliminar el archivo del almacenamiento", e);
        }
    }

    @Override
    public String generarUrlFirmada(String ruta, int expirationSecs) {
        String signUrl = String.format("%s/storage/v1/object/sign/%s/%s",
            supabaseUrl, bucketEvidencias, ruta);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseServiceKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"expiresIn\": %d}", expirationSecs);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<java.util.Map> response =
                restTemplate.exchange(signUrl, HttpMethod.POST, request, java.util.Map.class);

            if (response.getBody() != null && response.getBody().containsKey("signedURL")) {
                return supabaseUrl + response.getBody().get("signedURL").toString();
            }
            throw new RuntimeException("No se pudo obtener la URL firmada");
        } catch (Exception e) {
            log.error("Error generando URL firmada: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar URL firmada", e);
        }
    }
}
