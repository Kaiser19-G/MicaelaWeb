package com.colegio.bastidas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entidad de Usuario del sistema (autenticación y autorización).
 * Roles: DIRECTOR, DOCENTE, ALUMNO, ADMIN.
 */
@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuario_username", columnList = "username", unique = true),
    @Index(name = "idx_usuario_email", columnList = "email", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    @NotBlank
    private String username;

    @Column(nullable = false)
    @NotBlank
    private String password;

    @Column(unique = true, length = 100)
    @Email
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull
    private Rol rol;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "primer_login")
    @Builder.Default
    private Boolean primerLogin = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Spring Security ───────────────────────────────────────────────────
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()              { return Boolean.TRUE.equals(activo); }

    // ── Enum ──────────────────────────────────────────────────────────────
    public enum Rol {
        DIRECTOR,
        DOCENTE,
        ALUMNO,
        ADMIN
    }
}
