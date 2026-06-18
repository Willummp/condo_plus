package com.condoplus.notificacao.canal;

import com.condoplus.notificacao.config.properties.NotificacaoProperties;
import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.Notificacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailCanal implements CanalEntrega {
    private final NotificacaoProperties properties;

    @Override
    public Canal canalAtendido() {
        return Canal.EMAIL;
    }

    @Override
    public Mono<ResultadoEnvio> enviar(Notificacao notificacao) {
        if (!properties.canais().email().habilitado()) {
            return Mono.just(new ResultadoEnvio.Falha(
                    LocalDateTime.now(),
                    "Canal EMAIL desabilitado por configuração",
                    false));
        }

        return Mono.fromCallable(() -> {
                    log.info("[MOCK] Enviando EMAIL. destinatarioPessoaId={} titulo={}",
                            notificacao.getDestinatarioPessoaId(), notificacao.getTitulo());
                    Thread.sleep(50);
                    return (ResultadoEnvio) new ResultadoEnvio.Sucesso(
                            LocalDateTime.now(),
                            "msg-id-" + notificacao.getId());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> Mono.just(new ResultadoEnvio.Falha(
                        LocalDateTime.now(),
                        "Erro inesperado: " + ex.getMessage(),
                        true)));
    }
}
