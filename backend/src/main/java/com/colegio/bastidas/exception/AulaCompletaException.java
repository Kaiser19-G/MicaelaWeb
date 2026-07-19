package com.colegio.bastidas.exception;

/**
 * Se lanza cuando se intenta matricular un alumno en un Aula sin vacantes disponibles.
 * Los controllers que puedan disparar esta excepción deben mapearla a HTTP 409 (Conflict).
 */
public class AulaCompletaException extends RuntimeException {
    public AulaCompletaException(String mensaje) {
        super(mensaje);
    }
}
