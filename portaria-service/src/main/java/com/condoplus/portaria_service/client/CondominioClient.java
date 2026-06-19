package com.condoplus.portaria_service.client;

import com.condoplus.portaria_service.dto.client.PessoaExterno;
import com.condoplus.portaria_service.dto.client.VeiculoExterno;
import com.condoplus.portaria_service.exception.CondominioServiceException;
import com.condoplus.portaria_service.exception.CondominioServiceIndisponivelException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class CondominioClient {

    private final RestClient.Builder builder;
    private RestClient client;

    public CondominioClient(@Qualifier("balancedRestClient") RestClient.Builder builder) {
        this.builder = builder;
    }

    @PostConstruct
    void init() {
        this.client = builder.baseUrl("http://condominio-service").build();
    }

    @CircuitBreaker(name = "condominioService", fallbackMethod = "buscarVeiculoFallback")
    @TimeLimiter(name = "condominioService")
    public CompletableFuture<Optional<VeiculoExterno>> buscarVeiculoPorPlacaAsync(String placa) {
        return CompletableFuture.supplyAsync(() -> buscarVeiculoSync(placa));
    }

    @CircuitBreaker(name = "condominioService", fallbackMethod = "buscarPessoaFallback")
    @TimeLimiter(name = "condominioService")
    public CompletableFuture<Optional<PessoaExterno>> buscarPessoaAsync(UUID id) {
        return CompletableFuture.supplyAsync(() -> buscarPessoaSync(id));
    }

    private Optional<VeiculoExterno> buscarVeiculoSync(String placa) {
        try {
            VeiculoExterno v = client.get()
                    .uri("/condominio/veiculos?placa={p}", placa)
                    .retrieve()
                    .body(VeiculoExterno.class);
            return Optional.ofNullable(v);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().equals(HttpStatusCode.valueOf(404))) {
                return Optional.empty();
            }
            log.warn("Erro ao buscar veículo. status={}", e.getStatusCode());
            throw new CondominioServiceException("Falha ao buscar veículo", e);
        }
    }

    private Optional<PessoaExterno> buscarPessoaSync(UUID id) {
        try {
            PessoaExterno p = client.get()
                    .uri("/condominio/pessoas/{id}", id)
                    .retrieve()
                    .body(PessoaExterno.class);
            return Optional.ofNullable(p);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().equals(HttpStatusCode.valueOf(404))) {
                return Optional.empty();
            }
            throw new CondominioServiceException("Falha ao buscar pessoa", e);
        }
    }

    private CompletableFuture<Optional<VeiculoExterno>> buscarVeiculoFallback(
            String placa, Throwable t) {
        log.warn("Circuit Breaker ATIVO para buscarVeiculo. placa={}", placa, t);
        return CompletableFuture.failedFuture(
                new CondominioServiceIndisponivelException("condominio-service indisponível"));
    }

    private CompletableFuture<Optional<PessoaExterno>> buscarPessoaFallback(
            UUID id, Throwable t) {
        log.warn("Circuit Breaker ATIVO para buscarPessoa. id={}", id, t);
        return CompletableFuture.failedFuture(
                new CondominioServiceIndisponivelException("condominio-service indisponível"));
    }
}
