package com.colegio.bastidas.service;

import com.colegio.bastidas.model.EntregaTarea;
import com.colegio.bastidas.model.Tarea;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface TareaService {
    Tarea crearTarea(Long cursoId, Integer semana, String titulo, String descripcion, LocalDateTime fechaLimite, Long docenteId);
    List<Tarea> obtenerTareasPorCursoYSemana(Long cursoId, Integer semana);
    EntregaTarea subirEntrega(Long tareaId, Long alumnoId, MultipartFile archivo);
    List<EntregaTarea> obtenerEntregasPorTarea(Long tareaId);
    EntregaTarea calificarEntrega(Long entregaId, Double nota, String comentario);
}
