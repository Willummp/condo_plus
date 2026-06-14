package com.condoplus.iam.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
@Entity
@Table(name = "credencial_usuario", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CredencialUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    @Column(name = "senha_hash", nullable = false, length = 60)
    private String senhaHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusCredencial status;
    @Column(name = "tentativas_falhas", nullable = false)
    @Builder.Default
    private int tentativasFalhas = 0;
    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;
    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            schema = "iam",
            name = "credencial_role",
            joinColumns = @JoinColumn(name = "credencial_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    @PrePersist
    void onCreate() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusCredencial.ATIVO;
        }
    }
}
