package com.condoplus.notificacao.canal;

import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.Notificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Component
public class WhatsAppCanal implements CanalEntrega {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCanal.class);

    @Override
    public Canal canalAtendido() {
        return Canal.WHATSAPP;
    }

    @Override
    public Mono<ResultadoEnvio> enviar(Notificacao n) {
        log.info("[MOCK] WhatsApp: {} -> {}", n.getDestinatarioPessoaId(), n.getTitulo());
        return Mono.just(new ResultadoEnvio.Sucesso(LocalDateTime.now(), "wa-" + n.getId()));
    }
}