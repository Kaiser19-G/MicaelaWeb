package com.colegio.bastidas.util;

/**
 * Genera las credenciales iniciales (predecibles) para las cuentas de Usuario
 * que se crean automáticamente al matricular un alumno o contratar un docente.
 *
 * <p>El usuario debe cambiar esta contraseña en su primer login
 * (ver {@link com.colegio.bastidas.model.Usuario#getPrimerLogin()} y
 * {@code AuthController.changePassword}, que sí exige {@link PasswordPolicy}).
 */
public final class CredencialesGenerator {

    private CredencialesGenerator() {
    }

    public static String generarUsername(String dni) {
        return dni;
    }

    /**
     * Ej: dni="71512635", apellidoPaterno="HUAMAN" -&gt; "71512635Huaman".
     */
    public static String generarPasswordInicial(String dni, String apellidoPaterno) {
        String apellido = apellidoPaterno == null ? "" : apellidoPaterno.trim();
        String capitalizado = "";
        if (!apellido.isEmpty()) {
            capitalizado = apellido.substring(0, 1).toUpperCase()
                + apellido.substring(1).toLowerCase();
            capitalizado = capitalizado.replaceAll("\\s+", "");
        }
        return dni + capitalizado;
    }
}
