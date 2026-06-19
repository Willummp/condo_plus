package com.condoplus.portaria_service.service;

import com.condoplus.portaria_service.client.CondominioClient;
import com.condoplus.portaria_service.dto.client.VeiculoExterno;
import com.condoplus.portaria_service.dto.request.EntradaMoradorRequest;
import com.condoplus.portaria_service.dto.request.EntradaVisitanteRequest;
import com.condoplus.portaria_service.dto.request.SaidaRequest;
import com.condoplus.portaria_service.dto.response.RegistroAcessoResponseDTO;
import com.condoplus.portaria_service.exception.CondominioServiceIndisponivelException;
import com.condoplus.portaria_service.exception.PlacaNaoCadastradaException;
import com.condoplus.portaria_service.exception.PlacaNaoPertenceUnidadeException;
import com.condoplus.portaria_service.exception.VisitanteNaoAtivoException;
import com.condoplus.portaria_service.exception.VisitanteNaoEncontradoException;
import com.condoplus.portaria_service.messaging.EventoPublicador;
import com.condoplus.portaria_service.model.entities.RegistroAcesso;
import com.condoplus.portaria_service.model.entities.Visitante;
import com.condoplus.portaria_service.model.enums.TipoMovimento;
import com.condoplus.portaria_service.model.enums.TipoPessoaAcesso;
import com.condoplus.portaria_service.model.enums.TipoVisitante;
import com.condoplus.portaria_service.repository.RegistroAcessoRepository;
import com.condoplus.portaria_service.repository.VisitanteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistroAcessoService {

    private final RegistroAcessoRepository registroRepository;
    private final VisitanteRepository visitanteRepository;
    private final CondominioClient condominioClient;
    private final EventoPublicador eventoPublicador;

    @Value("${condoplus.kafka.topics.acessos-registrados}")
    private String topicoAcessos;

    @Transactional
    public RegistroAcessoResponseDTO registrarEntradaMorador(EntradaMoradorRequest req, UUID porteiroId) {
        if (req.veiculoPlaca() != null && !req.veiculoPlaca().isBlank()) {
            validarPlaca(req.veiculoPlaca(), req.unidadeId());
        }

        RegistroAcesso registro = RegistroAcesso.builder()
                .tipoPessoa(TipoPessoaAcesso.MORADOR)
                .pessoaId(req.moradorId())
                .unidadeId(req.unidadeId())
                .veiculoPlaca(req.veiculoPlaca())
                .tipoMovimento(TipoMovimento.ENTRADA)
                .porteiroId(porteiroId)
                .observacoes(req.observacoes())
                .build();

        RegistroAcesso salvo = registroRepository.save(registro);
        publicarAcesso(salvo);

        log.info("Entrada de morador registrada. id={} pessoaId={}", salvo.getId(), salvo.getPessoaId());
        return RegistroAcessoResponseDTO.fromEntity(salvo);
    }

    @Transactional
    public RegistroAcessoResponseDTO registrarEntradaVisitante(EntradaVisitanteRequest req, UUID porteiroId) {
        Visitante visitante = visitanteRepository.findById(req.visitanteId())
                .orElseThrow(() -> new VisitanteNaoEncontradoException(req.visitanteId()));

        if (!visitante.estaAtivo(LocalDateTime.now())) {
            throw new VisitanteNaoAtivoException(visitante.getId());
        }

        TipoPessoaAcesso tipoPessoa = visitante.getTipo() == TipoVisitante.PRESTADOR
                ? TipoPessoaAcesso.PRESTADOR
                : TipoPessoaAcesso.VISITANTE;

        RegistroAcesso registro = RegistroAcesso.builder()
                .tipoPessoa(tipoPessoa)
                .pessoaId(visitante.getId())
                .unidadeId(visitante.getAutorizadoParaUnidadeId())
                .tipoMovimento(TipoMovimento.ENTRADA)
                .porteiroId(porteiroId)
                .observacoes(req.observacoes())
                .build();

        RegistroAcesso salvo = registroRepository.save(registro);
        publicarAcesso(salvo);

        log.info("Entrada de visitante registrada. id={} visitanteId={}", salvo.getId(), visitante.getId());
        return RegistroAcessoResponseDTO.fromEntity(salvo);
    }

    @Transactional
    public RegistroAcessoResponseDTO registrarSaida(SaidaRequest req, UUID porteiroId) {
        RegistroAcesso registro = RegistroAcesso.builder()
                .tipoPessoa(req.tipoPessoa())
                .pessoaId(req.pessoaId())
                .unidadeId(req.unidadeId())
                .veiculoPlaca(req.veiculoPlaca())
                .tipoMovimento(TipoMovimento.SAIDA)
                .porteiroId(porteiroId)
                .observacoes(req.observacoes())
                .build();

        RegistroAcesso salvo = registroRepository.save(registro);
        publicarAcesso(salvo);

        log.info("Saída registrada. id={} pessoaId={}", salvo.getId(), salvo.getPessoaId());
        return RegistroAcessoResponseDTO.fromEntity(salvo);
    }

    private void publicarAcesso(RegistroAcesso salvo) {
        Map<String, Object> evento = new HashMap<>();
        evento.put("eventId",        UUID.randomUUID().toString());
        evento.put("occurredAt",     Instant.now().toString());
        evento.put("registroId",     salvo.getId().toString());
        evento.put("tipoPessoa",     salvo.getTipoPessoa().name());
        evento.put("pessoaId",       salvo.getPessoaId().toString());
        evento.put("unidadeId",      salvo.getUnidadeId() != null ? salvo.getUnidadeId().toString() : null);
        evento.put("tipoMovimento",  salvo.getTipoMovimento().name());
        evento.put("veiculoPlaca",   salvo.getVeiculoPlaca());

        eventoPublicador.publicar(topicoAcessos, salvo.getId().toString(), evento);
    }

    private void validarPlaca(String placa, UUID unidadeId) {
        try {
            Optional<VeiculoExterno> veiculo = condominioClient
                    .buscarVeiculoPorPlacaAsync(placa).get();

            if (veiculo.isEmpty()) throw new PlacaNaoCadastradaException(placa);
            if (!veiculo.get().unidadeId().equals(unidadeId))
                throw new PlacaNaoPertenceUnidadeException(placa, unidadeId);

        } catch (CondominioServiceIndisponivelException e) {
            log.warn("Modo degradado: condominio indisponível. placa={}", placa);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CondominioServiceIndisponivelException)
                log.warn("Modo degradado por indisponibilidade. placa={}", placa);
            else
                throw new IllegalStateException("Falha ao validar placa", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido ao validar placa", e);
        }
    }
}