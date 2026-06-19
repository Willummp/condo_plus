package com.condoplus.iam.repository;

import com.condoplus.iam.domain.Role;
import com.condoplus.iam.domain.TipoRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNome(TipoRole nome);
}
