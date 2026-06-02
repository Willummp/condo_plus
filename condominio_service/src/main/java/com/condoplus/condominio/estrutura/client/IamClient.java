package com.condoplus.condominio.estrutura.client;

import com.condoplus.condominio.estrutura.dto.CredencialResponse;
import com.condoplus.condominio.estrutura.dto.CriarCredencialRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletableFuture;

/**
 * Cliente HTTP resiliente encarregado de realizar a integração com o microserviço de IAM (iam-service).
 * 
 * <p>Esta classe gerencia a comunicação REST para criação de credenciais de acesso de forma assíncrona,
 * aplicando padrões de tolerância a falhas (Fault Tolerance) como Circuit Breaker e Time Limiter via Resilience4j.
 * 
 * <p>Anotações importantes:
 * <ul>
 *   <li>{@code @Component} — Declara esta classe como um componente Spring gerenciado para fins de injeção de dependência.</li>
 *   <li>{@code @Slf4j} — Injeta automaticamente o Logger SLF4J do Lombok.</li>
 * </ul>
 */
@Component
@Slf4j
public class IamClient {

    /**
     * Instância configurada do Spring HTTP RestClient utilizado para requisições.
     */
    private final RestClient restClient;

    /**
     * Construtor da classe que configura o RestClient com balanceamento de carga e URL base dinâmicos.
     * 
     * @param restClientBuilder Builder do RestClient qualificado para chamadas load-balanced.
     * @param iamBaseUrl URL base resolvida do arquivo de propriedades da aplicação.
     */
    public IamClient(
            @Qualifier("balancedRestClient") RestClient.Builder restClientBuilder,
            @Value("${condoplus.iam.base-url}") String iamBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(iamBaseUrl).build();
    }

    /**
     * Solicita assincronamente ao iam-service a criação de uma nova credencial de segurança.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @CircuitBreaker} — Monitora a taxa de falhas. Caso o iam-service apresente sucessivos erros ou timeout,
     *   o circuito se abre e as requisições passam a invocar diretamente o método de fallback {@link #fallbackCriarCredencial(CriarCredencialRequest, Exception)}
     *   sem sobrecarregar o microserviço de IAM.</li>
     *   <li>{@code @TimeLimiter} — Limita o tempo de espera máximo para o retorno da API. O Resilience4j exige o uso de
     *   retorno assíncrono (como {@link CompletableFuture}) para delegar a interrupção por timeout a uma threadpool separada.</li>
     * </ul>
     * 
     * @param request DTO com as informações de login (e-mail, senha inicial e papel).
     * @return Um {@link CompletableFuture} contendo a resposta com o UUID gerado no IAM.
     */
    @CircuitBreaker(name = "iamService", fallbackMethod = "fallbackCriarCredencial")
    @TimeLimiter(name = "iamService")
    public CompletableFuture<CredencialResponse> criarCredencial(CriarCredencialRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Chamando iam-service para criar credencial: email={}", request.email());
            return restClient.post()
                    .uri("/credenciais")
                    .body(request)
                    .retrieve()
                    .body(CredencialResponse.class);
        });
    }

    /**
     * Método de contingência (Fallback) acionado automaticamente caso a chamada principal ao iam-service
     * falhe repetidamente, estoure o tempo limite ou o circuito esteja no estado "ABERTO".
     * 
     * @param request O DTO original de criação da credencial.
     * @param ex A exceção original capturada que causou a falha na chamada HTTP.
     * @return Um {@link CompletableFuture} falhado contendo uma mensagem amigável de indisponibilidade do serviço.
     */
    public CompletableFuture<CredencialResponse> fallbackCriarCredencial(
            CriarCredencialRequest request, Exception ex) {
        log.error("Falha ao criar credencial no iam-service (fallback ativado). Erro: {}", ex.getMessage());
        return CompletableFuture.failedFuture(
                new RuntimeException("Serviço de Autenticação (IAM) temporariamente indisponível. Tente novamente mais tarde.")
        );
    }
}
