package com.condoplus.notificacao.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CondominioWebClient {

    private final WebClient webClient;

    public Flux<UUID> listarPessoasDaUnidade(UUID unidadeId) {
        log.info("Chamando CONDOMINIO-SERVICE via WebClient para unidadeId={}", unidadeId);
        return webClient.get()
                .uri("http://condominio-service/api/unidades/{id}/pessoas", unidadeId)
                .retrieve()
                .bodyToFlux(UUID.class);
    }

    public Flux<UUID> listarTodosMoradoresAtivos() {
        log.info("Chamando CONDOMINIO-SERVICE via WebClient para listar todos moradores ativos");
        return webClient.get()
                .uri("http://condominio-service/api/moradores/ativos")
                .retrieve()
                .bodyToFlux(UUID.class);
    }
}
