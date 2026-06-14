package com.colegio.bastidas.controller;

import com.colegio.bastidas.model.Usuario;
import com.colegio.bastidas.repository.UsuarioRepository;
import com.colegio.bastidas.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

/**
 * Controlador de autenticación JWT.
 * Endpoints base: {@code /api/v1/auth}
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "${app.cors.allowed-origins}", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /**
     * POST /auth/login
     * Autentifica al usuario y retorna el token JWT con datos del perfil.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        log.info("Intento de login: usuario={}", request.username());

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generarToken(userDetails);

        Usuario usuario = usuarioRepository.findByUsername(request.username())
            .orElseThrow();

        return ResponseEntity.ok(Map.of(
            "token", token,
            "tipo", "Bearer",
            "username", usuario.getUsername(),
            "rol", usuario.getRol().name(),
            "primerLogin", usuario.getPrimerLogin()
        ));
    }

    /**
     * POST /auth/logout
     * Invalidación del token en el cliente (stateless: el cliente elimina el token).
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente"));
    }

    /**
     * GET /auth/hash?texto=miContraseña  ← SOLO PARA DESARROLLO, eliminar en producción
     * Genera el hash BCrypt de un texto (útil para configurar contraseñas iniciales).
     */
    @GetMapping("/hash")
    public ResponseEntity<Map<String, String>> generarHash(@RequestParam String texto) {
        String hash = passwordEncoder.encode(texto);
        log.info("Hash generado para uso en BD");
        return ResponseEntity.ok(Map.of("hash", hash, "texto", texto));
    }

    /**
     * PUT /auth/change-password
     * Cambia la contraseña (obligatorio si primerLogin es true).
     */
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request) {

        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setPassword(passwordEncoder.encode(request.newPassword()));
        usuario.setPrimerLogin(false);
        usuarioRepository.save(usuario);

        log.info("Usuario {} actualizó su contraseña (primer login completado)", username);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada exitosamente"));
    }

    record LoginRequest(String username, String password) {}
    record ChangePasswordRequest(String newPassword) {}
}
