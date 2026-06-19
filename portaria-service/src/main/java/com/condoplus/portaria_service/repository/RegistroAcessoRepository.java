package com.condoplus.portaria_service.repository;

import com.condoplus.portaria_service.model.entities.RegistroAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RegistroAcessoRepository extends JpaRepository<RegistroAcesso, UUID> {

    List<RegistroAcesso> findByUnidadeIdAndTimestampAcessoBetween(
            UUID unidadeId, LocalDateTime inicio, LocalDateTime fim);

    List<RegistroAcesso> findByPessoaIdOrderByTimestampAcessoDesc(UUID pessoaId);
}
