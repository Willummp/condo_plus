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

    /**
     * Retorna os credencialIds (IAM UUID) dos moradores ativos de uma unidade.
     * Chama o condominio-service diretamente (service-to-service via Eureka/LB).
     * O path deve bater com o controller do condominio-service (sem prefixo /api/).
     */
    public Flux<UUID> listarPessoasDaUnidade(UUID unidadeId) {
        log.info("Chamando CONDOMINIO-SERVICE via WebClient para unidadeId={}", unidadeId);
        return webClient.get()
                .uri("lb://condominio-service/condominio/unidades/{id}/moradores/credenciais", unidadeId)
                .retrieve()
                .bodyToFlux(UUID.class)
                .onErrorResume(ex -> {
                    log.warn("Falha ao buscar credenciais da unidade {}: {}", unidadeId, ex.getMessage());
                    return Flux.empty();
                });
    }

    public Flux<UUID> listarTodosMoradoresAtivos() {
        log.info("Chamando CONDOMINIO-SERVICE via WebClient para listar todos moradores ativos");
        // Endpoint nao implementado — retorna vazio de forma segura
        return Flux.empty();
    }
}
