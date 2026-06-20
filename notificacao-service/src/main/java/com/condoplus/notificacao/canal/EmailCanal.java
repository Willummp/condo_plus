package com.condoplus.notificacao.canal;

import com.condoplus.notificacao.config.properties.NotificacaoProperties;
import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.Notificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.time.LocalDateTime;

@Component
public class EmailCanal implements CanalEntrega {

    private static final Logger log = LoggerFactory.getLogger(EmailCanal.class);
    private final NotificacaoProperties properties;

    public EmailCanal(NotificacaoProperties properties) {
        this.properties = properties;
    }

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
