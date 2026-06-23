package com.condoplus.condominio.estrutura.client;

import com.condoplus.condominio.estrutura.dto.CredencialResponse;
import com.condoplus.condominio.estrutura.dto.CriarCredencialRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class IamClient {

    
    private final WebClient webClient;

    
    public IamClient(
            @Qualifier("balancedWebClient") WebClient.Builder webClientBuilder,
            @Value("${condoplus.iam.base-url}") String iamBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(iamBaseUrl).build();
    }

    
    @CircuitBreaker(name = "iamService", fallbackMethod = "fallbackCriarCredencial")
    @TimeLimiter(name = "iamService")
    public Mono<CredencialResponse> criarCredencial(CriarCredencialRequest request) {
        log.info("Chamando iam-service de forma reativa para criar credencial: email={}", request.email());
        HttpServletRequest servletRequest =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return webClient.post()
                .uri("/credenciais")
                .header("X-User-Id",    servletRequest.getHeader("X-User-Id"))
                .header("X-User-Email", servletRequest.getHeader("X-User-Email"))
                .header("X-User-Roles", servletRequest.getHeader("X-User-Roles"))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CredencialResponse.class);
    }

    
    public Mono<CredencialResponse> fallbackCriarCredencial(
            CriarCredencialRequest request, Exception ex) {
        log.error("Falha ao criar credencial no iam-service reativo (fallback ativado). Erro: {}", ex.getMessage());
        return Mono.error(
                new RuntimeException("Serviço de Autenticação (IAM) temporariamente indisponível. Tente novamente mais tarde.")
        );
    }
}
