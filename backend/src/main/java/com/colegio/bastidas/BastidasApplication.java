package com.colegio.bastidas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal de arranque del Sistema de Gestión Escolar
 * I.E. Micaela Bastidas Puyocahua – La Tinguiña, Ica.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BastidasApplication {

    public static void main(String[] args) {
        SpringApplication.run(BastidasApplication.class, args);
    }
}
