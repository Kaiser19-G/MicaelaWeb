package com.colegio.bastidas.service;

import com.colegio.bastidas.model.MaterialSemana;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface MaterialService {
    MaterialSemana subirMaterial(Long cursoAsignadoId, Integer semana, MultipartFile archivo, Long docenteId);
    List<MaterialSemana> obtenerMaterialesPorCursoYSemana(Long cursoAsignadoId, Integer semana);
    List<MaterialSemana> obtenerMaterialesPorCurso(Long cursoAsignadoId);
    MaterialSemana obtenerPorId(Long materialId);
    void eliminarMaterial(Long materialId);
}
