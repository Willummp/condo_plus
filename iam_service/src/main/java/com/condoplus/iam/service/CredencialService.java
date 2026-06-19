package com.condoplus.iam.service;

import com.condoplus.iam.domain.CredencialUsuario;
import com.condoplus.iam.domain.Role;
import com.condoplus.iam.domain.StatusCredencial;
import com.condoplus.iam.domain.TipoRole;
import com.condoplus.iam.dto.AlteracaoStatusRequest;
import com.condoplus.iam.dto.CredencialResponse;
import com.condoplus.iam.dto.NovaCredencialRequest;
import com.condoplus.iam.event.CredencialCriadaEvent;
import com.condoplus.iam.exception.CredencialNaoEncontradaException;
import com.condoplus.iam.exception.EmailJaExisteException;
import com.condoplus.iam.repository.CredencialRepository;
import com.condoplus.iam.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CredencialService {

    private final CredencialRepository credencialRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventoPublicador eventoPublicador;

    @Transactional
    public CredencialResponse criar(NovaCredencialRequest req) {

        if (credencialRepository.existsByEmail(req.email())) {
            throw new EmailJaExisteException(req.email());
        }

        Set<Role> rolesAnexadas = req.roles().stream()
                .map(tipo -> roleRepository.findByNome(tipo)
                        .orElseThrow(() -> new IllegalStateException(
                                "Role " + tipo + " não encontrada no banco (seed faltando)"
                        )))
                .collect(Collectors.toSet());

        CredencialUsuario nova = CredencialUsuario.builder()
                .email(req.email())
                .senhaHash(passwordEncoder.encode(req.senha()))
                .status(StatusCredencial.ATIVO)
                .roles(rolesAnexadas)
                .build();

        CredencialUsuario salva = credencialRepository.save(nova);

        log.info("Credencial criada. id={} email={}", salva.getId(), salva.getEmail());

        Set<String> rolesStr = salva.getRoles().stream()
                .map(r -> r.getNome().name())
                .collect(Collectors.toSet());

        eventoPublicador.publicarCredencialCriada(new CredencialCriadaEvent(
                salva.getId(),
                salva.getEmail(),
                rolesStr,
                MDC.get("correlationId")
        ));

        return toResponse(salva);
    }

    @Transactional(readOnly = true)
    public CredencialResponse buscar(UUID id) {

        CredencialUsuario cred = credencialRepository.findById(id)
                .orElseThrow(() -> new CredencialNaoEncontradaException(id));

        return toResponse(cred);
    }

    @Transactional
    public void alterarStatus(UUID id, AlteracaoStatusRequest req) {

        CredencialUsuario cred = credencialRepository.findById(id)
                .orElseThrow(() -> new CredencialNaoEncontradaException(id));

        StatusCredencial anterior = cred.getStatus();

        cred.setStatus(req.novoStatus());

        if (req.novoStatus() == StatusCredencial.ATIVO) {
            cred.setTentativasFalhas(0);
            cred.setBloqueadoAte(null);
        }

        credencialRepository.save(cred);

        log.warn(
                "Status de credencial alterado. id={} de={} para={} motivo={}",
                id, anterior, req.novoStatus(), req.motivo()
        );
    }

    private CredencialResponse toResponse(CredencialUsuario c) {

        Set<TipoRole> tipos = c.getRoles().stream()
                .map(Role::getNome)
                .collect(Collectors.toSet());

        return new CredencialResponse(
                c.getId(),
                c.getEmail(),
                c.getStatus(),
                tipos,
                c.getCriadoEm(),
                c.getUltimoLogin()
        );
    }
}
