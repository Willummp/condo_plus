package com.condoplus.condominio.estrutura.client;

import com.condoplus.condominio.estrutura.dto.CredencialResponse;
import com.condoplus.condominio.estrutura.dto.CriarCredencialRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente HTTP resiliente e reativo encarregado de realizar a integração com o microserviço de IAM (iam-service).
 * 
 * <p>Esta classe gerencia a comunicação REST para criação de credenciais de acesso de forma assíncrona/reativa,
 * aplicando padrões de tolerância a falhas (Fault Tolerance) como Circuit Breaker e Time Limiter via Resilience4j
 * integrados com o Project Reactor (Mono).
 */
@Component
@Slf4j
public class IamClient {

    /**
     * Instância configurada do WebClient reativo utilizado para requisições.
     */
    private final WebClient webClient;

    /**
     * Construtor da classe que configura o WebClient com balanceamento de carga e URL base dinâmicos.
     */
    public IamClient(
            @Qualifier("balancedWebClient") WebClient.Builder webClientBuilder,
            @Value("${condoplus.iam.base-url}") String iamBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(iamBaseUrl).build();
    }

    /**
     * Solicita reativamente ao iam-service a criação de uma nova credencial de segurança.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @CircuitBreaker} — Monitora a taxa de falhas do fluxo reativo.</li>
     *   <li>{@code @TimeLimiter} — Limita o tempo de resposta máximo da chamada. Integrado nativamente com WebFlux/Reactor.</li>
     * </ul>
     * 
     * @param request DTO com as informações de login (e-mail, senha inicial e papel).
     * @return Um {@link Mono} contendo a resposta com o UUID gerado no IAM.
     */
    @CircuitBreaker(name = "iamService", fallbackMethod = "fallbackCriarCredencial")
    @TimeLimiter(name = "iamService")
    public Mono<CredencialResponse> criarCredencial(CriarCredencialRequest request) {
        log.info("Chamando iam-service de forma reativa para criar credencial: email={}", request.email());
        return webClient.post()
                .uri("/credenciais")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CredencialResponse.class);
    }

    /**
     * Método de contingência (Fallback) acionado automaticamente caso a chamada principal ao iam-service
     * falhe repetidamente, estoure o tempo limite ou o circuito esteja no estado "ABERTO".
     * 
     * @param request O DTO original de criação da credencial.
     * @param ex A exceção original capturada que causou a falha na chamada HTTP.
     * @return Um {@link Mono} de erro contendo uma mensagem amigável de indisponibilidade do serviço.
     */
    public Mono<CredencialResponse> fallbackCriarCredencial(
            CriarCredencialRequest request, Exception ex) {
        log.error("Falha ao criar credencial no iam-service reativo (fallback ativado). Erro: {}", ex.getMessage());
        return Mono.error(
                new RuntimeException("Serviço de Autenticação (IAM) temporariamente indisponível. Tente novamente mais tarde.")
        );
    }
}
