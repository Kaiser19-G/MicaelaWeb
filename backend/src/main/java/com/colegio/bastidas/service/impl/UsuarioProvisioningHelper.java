package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.model.Usuario;
import com.colegio.bastidas.repository.UsuarioRepository;
import com.colegio.bastidas.util.CredencialesGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea la cuenta de {@link Usuario} que se aprovisiona automáticamente al
 * matricular un alumno o registrar un docente, evitando duplicar esta lógica
 * en {@code AlumnoServiceImpl}, {@code DocenteServiceImpl} y
 * {@code MatriculaServiceImpl}.
 *
 * <p>La contraseña inicial es predecible (DNI + apellido paterno, ver
 * {@link CredencialesGenerator}) y el usuario queda obligado a cambiarla en
 * su primer login ({@code primerLogin=true}, ya soportado por el flujo de
 * {@code AuthController.changePassword}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UsuarioProvisioningHelper {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Usuario crearUsuario(String dni, String apellidoPaterno, Usuario.Rol rol) {
        String username = CredencialesGenerator.generarUsername(dni);
        if (usuarioRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                "Ya existe una cuenta de usuario con el DNI " + dni);
        }

        String passwordInicial = CredencialesGenerator.generarPasswordInicial(dni, apellidoPaterno);

        Usuario usuario = Usuario.builder()
            .username(username)
            .password(passwordEncoder.encode(passwordInicial))
            .rol(rol)
            .activo(true)
            .primerLogin(true)
            .build();

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario {} aprovisionado automáticamente (rol={})", username, rol);
        return guardado;
    }
}
