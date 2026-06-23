package com.condoplus.notificacao.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Component
public class CondominioWebClient {

    private static final Logger log = LoggerFactory.getLogger(CondominioWebClient.class);
    private final WebClient webClient;

    public CondominioWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Flux<UUID> listarPessoasDaUnidade(UUID unidadId) {
        log.info("Chamando CONDOMINIO-SERVICE via WebClient para unidadeId={}", unidadId);
        return webClient.get()
                .uri("lb://condominio-service/api/unidades/{id}/pessoas", unidadId)
                .retrieve()
                .bodyToFlux(UUID.class);
    }

    public Flux<UUID> listarTodosMoradoresAtivos() {
        log.info("Chamando CONDOMINIO-SERVICE via WebClient para listar todos moradores ativos");
        return webClient.get()
                .uri("lb://condominio-service/api/moradores/ativos")
                .retrieve()
                .bodyToFlux(UUID.class);
    }
}
