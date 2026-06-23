package com.condoplus.iam.repository;

import com.condoplus.iam.domain.CredencialUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CredencialRepository extends JpaRepository<CredencialUsuario, UUID> {
    Optional<CredencialUsuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
