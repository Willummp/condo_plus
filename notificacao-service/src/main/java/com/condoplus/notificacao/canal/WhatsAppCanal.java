package com.condoplus.notificacao.canal;

import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.Notificacao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Component
@Slf4j
public class WhatsAppCanal implements CanalEntrega {
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
