package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.model.Alumno;
import com.colegio.bastidas.model.Docente;
import com.colegio.bastidas.model.EntregaTarea;
import com.colegio.bastidas.model.Tarea;
import com.colegio.bastidas.repository.AlumnoRepository;
import com.colegio.bastidas.repository.CursoAsignadoRepository;
import com.colegio.bastidas.repository.DocenteRepository;
import com.colegio.bastidas.repository.EntregaTareaRepository;
import com.colegio.bastidas.repository.TareaRepository;
import com.colegio.bastidas.service.SupabaseStorageService;
import com.colegio.bastidas.service.TareaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TareaServiceImpl implements TareaService {

    private final TareaRepository tareaRepository;
    private final EntregaTareaRepository entregaTareaRepository;
    private final DocenteRepository docenteRepository;
    private final AlumnoRepository alumnoRepository;
    private final SupabaseStorageService storageService;
    private final CursoAsignadoRepository cursoAsignadoRepository;

    @Override
    public Tarea crearTarea(Long cursoAsignadoId, Integer semana, String titulo, String descripcion, LocalDateTime fechaLimite, Long docenteId) {
        Docente docente = docenteRepository.findById(docenteId)
            .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado"));

        com.colegio.bastidas.model.CursoAsignado cursoAsignado = cursoAsignadoRepository.findById(cursoAsignadoId)
            .orElseThrow(() -> new IllegalArgumentException("Curso asignado no encontrado"));

        Tarea tarea = Tarea.builder()
            .cursoAsignado(cursoAsignado)
            .semana(semana)
            .titulo(titulo)
            .descripcion(descripcion)
            .fechaLimite(fechaLimite)
            .docente(docente)
            .build();
            
        return tareaRepository.save(tarea);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tarea> obtenerTareasPorCursoYSemana(Long cursoAsignadoId, Integer semana) {
        return tareaRepository.findByCursoAsignadoIdAndSemana(cursoAsignadoId, semana);
    }

    @Override
    public EntregaTarea subirEntrega(Long tareaId, Long alumnoId, MultipartFile archivo) {
        Tarea tarea = tareaRepository.findById(tareaId)
            .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
            
        Alumno alumno = alumnoRepository.findById(alumnoId)
            .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));

        if (LocalDateTime.now().isAfter(tarea.getFechaLimite())) {
            log.warn("Alumno {} intentó subir tarea fuera de plazo", alumnoId);
            // Dependiendo de la regla de negocio, podríamos lanzar excepción o permitirlo con penalidad.
            // Por ahora permitiremos subirlo, se guardará con la fecha de entrega real.
        }

        // USO DE APACHE COMMONS IO
        String extension = FilenameUtils.getExtension(archivo.getOriginalFilename());
        String nombreSeguro = UUID.randomUUID().toString() + "." + extension;
        String rutaSupabase = "tareas/" + tareaId + "/" + alumnoId + "/" + nombreSeguro;

        // Subir archivo a Supabase
        String urlPublica = storageService.subirArchivo(archivo, rutaSupabase);

        EntregaTarea entrega = entregaTareaRepository.findByTareaIdAndAlumnoId(tareaId, alumnoId)
            .orElse(EntregaTarea.builder()
                .tarea(tarea)
                .alumno(alumno)
                .build());

        entrega.setArchivoUrl(urlPublica);
        entrega.setNombreArchivo(archivo.getOriginalFilename());
        entrega.setFechaEntrega(LocalDateTime.now());
        
        return entregaTareaRepository.save(entrega);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntregaTarea> obtenerEntregasPorTarea(Long tareaId) {
        return entregaTareaRepository.findByTareaId(tareaId);
    }

    @Override
    public EntregaTarea calificarEntrega(Long entregaId, Double nota, String comentario) {
        EntregaTarea entrega = entregaTareaRepository.findById(entregaId)
            .orElseThrow(() -> new IllegalArgumentException("Entrega no encontrada"));
            
        if (nota != null) {
            entrega.setNotaAsignada(BigDecimal.valueOf(nota));
        }
        if (comentario != null) {
            entrega.setComentarioDocente(comentario);
        }
        
        return entregaTareaRepository.save(entrega);
    }
}
