package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.model.CursoAsignado;
import com.colegio.bastidas.model.Docente;
import com.colegio.bastidas.model.MaterialSemana;
import com.colegio.bastidas.repository.CursoAsignadoRepository;
import com.colegio.bastidas.repository.DocenteRepository;
import com.colegio.bastidas.repository.MaterialSemanaRepository;
import com.colegio.bastidas.service.MaterialService;
import com.colegio.bastidas.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MaterialServiceImpl implements MaterialService {

    private final MaterialSemanaRepository materialRepository;
    private final CursoAsignadoRepository cursoAsignadoRepository;
    private final DocenteRepository docenteRepository;
    private final SupabaseStorageService storageService;

    @Override
    public MaterialSemana subirMaterial(Long cursoAsignadoId, Integer semana, MultipartFile archivo, Long docenteId) {
        Docente docente = docenteRepository.findById(docenteId)
            .orElseThrow(() -> new IllegalArgumentException("Docente no encontrado"));

        CursoAsignado cursoAsignado = cursoAsignadoRepository.findById(cursoAsignadoId)
            .orElseThrow(() -> new IllegalArgumentException("Curso asignado no encontrado"));

        String extension = FilenameUtils.getExtension(archivo.getOriginalFilename());
        String nombreSeguro = UUID.randomUUID().toString() + "." + extension;
        String rutaSupabase = "materiales/" + cursoAsignadoId + "/semana" + semana + "/" + nombreSeguro;

        String urlPublica = storageService.subirArchivo(archivo, rutaSupabase);

        MaterialSemana material = MaterialSemana.builder()
            .cursoAsignado(cursoAsignado)
            .semana(semana)
            .docente(docente)
            .nombreArchivo(archivo.getOriginalFilename())
            .urlArchivo(urlPublica)
            .build();

        return materialRepository.save(material);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialSemana> obtenerMaterialesPorCursoYSemana(Long cursoAsignadoId, Integer semana) {
        return materialRepository.findByCursoAsignadoIdAndSemana(cursoAsignadoId, semana);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialSemana> obtenerMaterialesPorCurso(Long cursoAsignadoId) {
        return materialRepository.findByCursoAsignadoId(cursoAsignadoId);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialSemana obtenerPorId(Long materialId) {
        return materialRepository.findById(materialId)
            .orElseThrow(() -> new IllegalArgumentException("Material no encontrado: " + materialId));
    }

    @Override
    public void eliminarMaterial(Long materialId) {
        materialRepository.deleteById(materialId);
    }
}
