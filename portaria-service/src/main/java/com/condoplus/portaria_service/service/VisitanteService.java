package com.condoplus.portaria_service.service;

import com.condoplus.portaria_service.dto.request.NovoVisitanteRequest;
import com.condoplus.portaria_service.dto.response.VisitanteResponseDTO;
import com.condoplus.portaria_service.exception.AutorizacaoNegadaException;
import com.condoplus.portaria_service.exception.VisitanteNaoEncontradoException;
import com.condoplus.portaria_service.model.entities.Visitante;
import com.condoplus.portaria_service.model.enums.StatusVisitante;
import com.condoplus.portaria_service.model.enums.TipoVisitante;
import com.condoplus.portaria_service.repository.VisitanteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitanteService {

    private final VisitanteRepository visitanteRepository;

    @Transactional
    public VisitanteResponseDTO autorizar(NovoVisitanteRequest req, UUID pessoaAutorizadora) {

        // Regra de domínio: PRESTADOR só pode ser autorizado por SÍNDICO
        // Não usar @PreAuthorize no controller pois a regra é condicional ao tipo
        if (req.tipo() == TipoVisitante.PRESTADOR) {
            verificarRoleSindico();
        }

        if (!req.validadeFim().isAfter(req.validadeInicio())) {
            throw new IllegalArgumentException("Validade fim deve ser posterior a validade início");
        }

        Visitante visitante = Visitante.builder()
                .nome(req.nome())
                .documento(req.documento())
                .telefone(req.telefone())
                .tipo(req.tipo())
                .autorizadoPorPessoaId(pessoaAutorizadora)
                .autorizadoParaUnidadeId(req.unidadeId())
                .validadeInicio(req.validadeInicio())
                .validadeFim(req.validadeFim())
                .status(StatusVisitante.AUTORIZADO)
                .build();

        Visitante salvo = visitanteRepository.save(visitante);

        log.info("Visitante autorizado. id={} tipo={} unidadeId={} por={}",
                salvo.getId(), salvo.getTipo(), salvo.getAutorizadoParaUnidadeId(), pessoaAutorizadora);

        return VisitanteResponseDTO.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public List<VisitanteResponseDTO> listarPorUnidade(UUID unidadeId) {
        return visitanteRepository.findByAutorizadoParaUnidadeId(unidadeId)
                .stream()
                .map(VisitanteResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void encerrar(UUID visitanteId) {
        Visitante visitante = visitanteRepository.findById(visitanteId)
                .orElseThrow(() -> new VisitanteNaoEncontradoException(visitanteId));

        visitante.encerrar();
        visitanteRepository.save(visitante);

        log.info("Visitante encerrado manualmente. id={}", visitanteId);
    }

    /**
     * Verifica se o usuário autenticado tem role SÍNDICO ou ADMIN.
     * Lançado apenas quando tipo == PRESTADOR.
     */
    private void verificarRoleSindico() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getAuthorities() == null) {
            throw new AutorizacaoNegadaException(
                    "Apenas SÍNDICO pode autorizar visitante PRESTADOR");
        }

        boolean ehSindico = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_SINDICO") || role.equals("ROLE_ADMIN"));

        if (!ehSindico) {
            throw new AutorizacaoNegadaException(
                    "Apenas SÍNDICO pode autorizar visitante PRESTADOR");
        }
    }
}
