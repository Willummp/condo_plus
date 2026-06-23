package com.condoplus.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filtro global reativo para o API Gateway.
 * 
 * <p>Responsável por interceptar todas as requisições de entrada, verificar/gerar o
 * correlationId (UUID único) e propagá-lo através dos cabeçalhos HTTP para todos os 
 * microsserviços do ecossistema.
 * 
 * <p>Dessa forma, conseguimos correlacionar logs de diferentes microsserviços para uma mesma chamada externa.
 */
@Component
public class CorrelationGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationGatewayFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String correlationId;

        if (headers.containsKey(CORRELATION_ID_HEADER)) {
            correlationId = headers.getFirst(CORRELATION_ID_HEADER);
            log.debug("Correlation ID existente encontrado no Gateway: {}", correlationId);
        } else {
            correlationId = UUID.randomUUID().toString();
            log.debug("Correlation ID gerado no Gateway: {}", correlationId);
        }

        // Mutaciona a requisição adicionando o header para propagação downstream
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        // Adiciona o header na resposta HTTP para o cliente
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        // Executa com a mais alta prioridade possível para garantir que o ID esteja presente desde o início
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
