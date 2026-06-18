package com.condoplus.notificacao.canal;

import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.Notificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Component
public class InAppCanal implements CanalEntrega {

    private static final Logger log = LoggerFactory.getLogger(InAppCanal.class);

    @Override
    public Canal canalAtendido() {
        return Canal.IN_APP;
    }

    @Override
    public Mono<ResultadoEnvio> enviar(Notificacao n) {
        log.debug("In-app: registro disponível para fetch. id={}", n.getId());
        return Mono.just(new ResultadoEnvio.Sucesso(LocalDateTime.now(), "in-app"));
    }
}
