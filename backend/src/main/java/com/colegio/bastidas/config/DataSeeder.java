package com.colegio.bastidas.config;

import com.colegio.bastidas.model.Usuario;
import com.colegio.bastidas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository.findByUsername("alumno1").isEmpty()) {
            Usuario alumno = Usuario.builder()
                    .username("alumno1")
                    .password(passwordEncoder.encode("alumno123"))
                    .email("alumno1@micaelabastidas.edu.pe")
                    .rol(Usuario.Rol.ALUMNO)
                    .activo(true)
                    .primerLogin(false)
                    .build();
            usuarioRepository.save(alumno);
            log.info("Usuario ALUMNO creado: username=alumno1, password=alumno123");
        } else {
            log.info("Usuario ALUMNO (alumno1) ya existe en la base de datos.");
        }
    }
}
