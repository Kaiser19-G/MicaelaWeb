package com.colegio.bastidas.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Servicio de integración con Supabase Storage para la carga de evidencias,
 * fotos de exámenes y documentos de expedientes.
 */
public interface SupabaseStorageService {

    /**
     * Sube un archivo al bucket configurado en Supabase Storage.
     *
     * @param archivo Archivo multipart a subir
     * @param ruta    Ruta relativa dentro del bucket (ej. "evidencias/123/examen.jpg")
     * @return URL pública del archivo subido
     */
    String subirArchivo(MultipartFile archivo, String ruta);

    /**
     * Elimina un archivo de Supabase Storage.
     *
     * @param ruta Ruta relativa del archivo a eliminar
     */
    void eliminarArchivo(String ruta);

    /**
     * Genera una URL firmada con tiempo de expiración para acceso seguro.
     *
     * @param ruta           Ruta relativa del archivo
     * @param expirationSecs Segundos de validez de la URL firmada
     * @return URL firmada
     */
    String generarUrlFirmada(String ruta, int expirationSecs);
}
