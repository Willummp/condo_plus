package com.condoplus.notificacao.service;

import com.condoplus.notificacao.canal.CanalEntrega;
import com.condoplus.notificacao.canal.ResultadoEnvio;
import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.Notificacao;
import com.condoplus.notificacao.domain.StatusNotificacao;
import com.condoplus.notificacao.repository.NotificacaoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DespacharService {
    private final List<CanalEntrega> canais;
    private final NotificacaoRepository notificacaoRepository;
    private Map<Canal, CanalEntrega> canalPorTipo;

    @PostConstruct
    void inicializar() {
        canalPorTipo = new EnumMap<>(Canal.class);
        for (CanalEntrega c : canais) {
            canalPorTipo.put(c.canalAtendido(), c);
        }
        log.info("Canais de entrega registrados: {}", canalPorTipo.keySet());
    }

    public Mono<Notificacao> despachar(Notificacao notificacao) {
        CanalEntrega canal = canalPorTipo.get(notificacao.getCanal());
        if (canal == null) {
            log.error("Canal não implementado: {}", notificacao.getCanal());
            return marcarFalha(notificacao, "Canal não disponível", false);
        }
        return canal.enviar(notificacao)
                .flatMap(resultado -> switch (resultado) {
                    case ResultadoEnvio.Sucesso s -> marcarEnviada(notificacao);
                    case ResultadoEnvio.Falha f -> marcarFalha(
                            notificacao, f.mensagem(), f.retentavel());
                });
    }

    private Mono<Notificacao> marcarEnviada(Notificacao n) {
        n.setStatus(StatusNotificacao.ENVIADA);
        n.setEnviadaEm(LocalDateTime.now());
        n.setTentativas(n.getTentativas() + 1);
        return notificacaoRepository.save(n);
    }

    private Mono<Notificacao> marcarFalha(Notificacao n, String erro, boolean retentavel) {
        n.setUltimoErro(erro);
        n.setTentativas(n.getTentativas() + 1);
        n.setStatus(retentavel ? StatusNotificacao.PENDENTE : StatusNotificacao.FALHOU);
        return notificacaoRepository.save(n);
    }
}
