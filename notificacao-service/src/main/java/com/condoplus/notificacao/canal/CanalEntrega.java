package com.condoplus.notificacao.canal;

import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.Notificacao;
import reactor.core.publisher.Mono;

public interface CanalEntrega {
    Canal canalAtendido();
    Mono<ResultadoEnvio> enviar(Notificacao notificacao);
}
