package com.colegio.bastidas.service.impl;

import com.colegio.bastidas.dto.circular.CircularRequestDTO;
import com.colegio.bastidas.dto.circular.CircularResponseDTO;
import com.colegio.bastidas.model.Circular;
import com.colegio.bastidas.model.Usuario;
import com.colegio.bastidas.repository.CircularRepository;
import com.colegio.bastidas.repository.UsuarioRepository;
import com.colegio.bastidas.service.CircularService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CircularServiceImpl implements CircularService {

    private final CircularRepository circularRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CircularResponseDTO> listar() {
        return circularRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(CircularResponseDTO::fromEntity)
            .toList();
    }

    @Override
    public CircularResponseDTO crear(CircularRequestDTO dto, Authentication authentication) {
        Usuario autor = usuarioRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Circular.DirigidoA dirigidoA;
        try {
            dirigidoA = Circular.DirigidoA.valueOf(dto.getDirigidoA());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valor inválido para dirigidoA: " + dto.getDirigidoA());
        }

        Circular circular = Circular.builder()
            .titulo(dto.getTitulo())
            .contenido(dto.getContenido())
            .dirigidoA(dirigidoA)
            .publicadaPor(autor)
            .build();

        Circular guardado = circularRepository.save(circular);
        log.info("Circular creada como borrador: {} por {}", guardado.getTitulo(), autor.getUsername());
        return CircularResponseDTO.fromEntity(guardado);
    }

    @Override
    public CircularResponseDTO publicar(Long id) {
        Circular circular = circularRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Circular no encontrada con ID: " + id));

        circular.setPublicada(true);
        circular.setFechaPublicacion(LocalDateTime.now());
        Circular guardado = circularRepository.save(circular);

        log.info("Circular publicada: {}", guardado.getTitulo());
        return CircularResponseDTO.fromEntity(guardado);
    }
}
