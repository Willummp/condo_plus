package com.condoplus.notificacao.canal;

import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.Notificacao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Component
@Slf4j
public class InAppCanal implements CanalEntrega {
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
