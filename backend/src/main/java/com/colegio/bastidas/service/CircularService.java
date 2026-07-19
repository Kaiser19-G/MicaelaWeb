package com.colegio.bastidas.service;

import com.colegio.bastidas.dto.circular.CircularRequestDTO;
import com.colegio.bastidas.dto.circular.CircularResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface CircularService {

    List<CircularResponseDTO> listar();

    CircularResponseDTO crear(CircularRequestDTO dto, Authentication authentication);

    CircularResponseDTO publicar(Long id);
}
