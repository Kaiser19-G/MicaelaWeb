package com.colegio.bastidas.util;

import java.util.regex.Pattern;

/**
 * Política de complejidad exigida a las contraseñas elegidas por el usuario
 * (no aplica a la contraseña inicial predecible generada al crear la cuenta,
 * que el usuario está obligado a cambiar en su primer login).
 */
public final class PasswordPolicy {

    private static final Pattern MAYUSCULA = Pattern.compile("[A-ZÁÉÍÓÚÑ]");
    private static final Pattern MINUSCULA = Pattern.compile("[a-záéíóúñ]");
    private static final Pattern ESPECIAL = Pattern.compile("[^A-Za-z0-9ÁÉÍÓÚÑáéíóúñ]");
    private static final int LONGITUD_MINIMA = 8;

    private PasswordPolicy() {
    }

    public static boolean esValida(String password) {
        if (password == null || password.length() < LONGITUD_MINIMA) {
            return false;
        }
        return MAYUSCULA.matcher(password).find()
            && MINUSCULA.matcher(password).find()
            && ESPECIAL.matcher(password).find();
    }

    public static String mensajeError() {
        return "La contraseña debe tener al menos 8 caracteres, una letra mayúscula, "
            + "una minúscula y un carácter especial.";
    }
}
