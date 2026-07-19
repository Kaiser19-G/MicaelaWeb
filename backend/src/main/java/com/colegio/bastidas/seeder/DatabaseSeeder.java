package com.colegio.bastidas.seeder;

import com.colegio.bastidas.model.*;
import com.colegio.bastidas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeder para poblar la base de datos con datos de prueba solo si está vacía.
 *
 * <p>IMPORTANTE: este seeder NO debe truncar tablas en cada arranque. Antes
 * ejecutaba un TRUNCATE ... CASCADE incondicional que borraba toda la
 * información real (asistencias, tareas, materiales, notas) cada vez que el
 * backend se reiniciaba, dando la falsa impresión de que nada se guardaba.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;
    private final AlumnoRepository alumnoRepository;
    private final AulaRepository aulaRepository;
    private final CursoAsignadoRepository cursoAsignadoRepository;
    private final MatriculaRepository matriculaRepository;
    private final MaterialSemanaRepository materialSemanaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            log.info("DatabaseSeeder: la base de datos ya contiene datos, no se vuelve a poblar.");
            return;
        }

        log.info("Iniciando DatabaseSeeder...");

        String defaultPassword = passwordEncoder.encode("123456");

        // 1. Crear Director
        Usuario director = Usuario.builder()
                .username("11111111")
                .password(defaultPassword)
                .rol(Usuario.Rol.DIRECTOR)
                .activo(true)
                .build();
        usuarioRepository.save(director);

        // 2. Crear Aulas (5to Secundaria A, B, C, D)
        Aula aula5toA = Aula.builder().grado("5").seccion("A").nivel(Aula.Nivel.SECUNDARIA).anioAcademico(2026).capacidad(30).build();
        Aula aula5toB = Aula.builder().grado("5").seccion("B").nivel(Aula.Nivel.SECUNDARIA).anioAcademico(2026).capacidad(30).build();
        Aula aula5toC = Aula.builder().grado("5").seccion("C").nivel(Aula.Nivel.SECUNDARIA).anioAcademico(2026).capacidad(30).build();
        Aula aula5toD = Aula.builder().grado("5").seccion("D").nivel(Aula.Nivel.SECUNDARIA).anioAcademico(2026).capacidad(30).build();
        aula5toA = aulaRepository.save(aula5toA);
        aula5toB = aulaRepository.save(aula5toB);
        aula5toC = aulaRepository.save(aula5toC);
        aula5toD = aulaRepository.save(aula5toD);
        
        List<Aula> aulas5to = List.of(aula5toA, aula5toB, aula5toC, aula5toD);

        // 3. Crear 5 Docentes
        List<Docente> docentes = new ArrayList<>();
        String[] nombresDocentes = {"Matemática Docente", "Comunicación Docente", "Ciencias Docente", "Historia Docente", "Inglés Docente"};
        for (int i = 1; i <= 5; i++) {
            String dni = "2222222" + i;
            Usuario userDocente = Usuario.builder()
                    .username(dni)
                    .password(defaultPassword)
                    .rol(Usuario.Rol.DOCENTE)
                    .activo(true)
                    .build();
            userDocente = usuarioRepository.save(userDocente);

            Docente docente = Docente.builder()
                    .usuario(userDocente)
                    .nombres(nombresDocentes[i-1])
                    .apellidoPaterno("Paterno " + i)
                    .apellidoMaterno("Materno " + i)
                    .dni(dni)
                    .codigoDocente("DOC-" + i)
                    .emailInstitucional("docente" + i + "@bastidas.edu.pe")
                    .celular("99988877" + i)
                    .build();
            docentes.add(docenteRepository.save(docente));
        }

        // 4. Asignar Cursos a Docentes en todas las aulas de 5to
        String[] materias = {"Matemática", "Comunicación", "Ciencias", "Historia", "Inglés"};
        for (int i = 0; i < 5; i++) {
            Docente docente = docentes.get(i);
            String materia = materias[i];
            
            for (Aula aula : aulas5to) {
                CursoAsignado ca = CursoAsignado.builder()
                        .docente(docente)
                        .aula(aula)
                        .areaCurricular(materia)
                        .anioAcademico(2026)
                        .build();
                ca = cursoAsignadoRepository.save(ca);

                // Crear materiales de prueba para cada curso (Semanas 1 a 3)
                for (int semana = 1; semana <= 3; semana++) {
                    materialSemanaRepository.save(MaterialSemana.builder()
                            .cursoAsignado(ca)
                            .semana(semana)
                            .nombreArchivo("Sílabo y Guía - Semana " + semana + " (" + materia + " 5to " + aula.getSeccion() + ").pdf")
                            .urlArchivo("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                            .docente(docente)
                            .build());
                }
            }
        }

        // 5. Crear 20 Alumnos y matricularlos (5 por aula de 5to A, B, C, D)
        for (int i = 1; i <= 20; i++) {
            String dni = "333333" + String.format("%02d", i);
            Usuario userAlumno = Usuario.builder()
                    .username(dni)
                    .password(defaultPassword)
                    .rol(Usuario.Rol.ALUMNO)
                    .activo(true)
                    .build();
            userAlumno = usuarioRepository.save(userAlumno);

            Aula aulaMatricula = aulas5to.get((i - 1) % 4);

            Alumno alumno = Alumno.builder()
                    .usuario(userAlumno)
                    .nombres("Alumno " + i)
                    .apellidoPaterno("Estudiante " + i)
                    .apellidoMaterno("Paterno")
                    .dni(dni)
                    .codigoEstudiante("EST-" + i)
                    .fechaNacimiento(LocalDate.of(2010, 1, (i % 28) + 1))
                    .aula(aulaMatricula) // <--- FIX AQUI: Asignar el aula directamente al alumno
                    .build();
            alumno = alumnoRepository.save(alumno);

            Matricula matricula = Matricula.builder()
                    .alumno(alumno)
                    .grado(aulaMatricula.getGrado())
                    .seccion(aulaMatricula.getSeccion())
                    .anioEscolar(2026)
                    .estado(Matricula.EstadoMatricula.ACTIVO)
                    .build();
            matriculaRepository.save(matricula);
        }

        log.info("DatabaseSeeder completado con éxito.");
    }
}
