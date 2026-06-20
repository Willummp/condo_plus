package com.condoplus.auditoria.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Garante que TODA requisicao REST carregue um correlationId no MDC, para os
 * logs serem rastreaveis (igual ja acontece no consumer Kafka desde o C16).
 *
 * Le o header X-Correlation-ID injetado pelo Gateway; se vier ausente (ex:
 * chamada direta em dev), gera um novo. Devolve o id no header da resposta
 * para o chamador conseguir rastrear. Limpa o MDC ao fim (o pool de threads
 * do Tomcat e reutilizado — nao limpar vazaria o id para a proxima requisicao).
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}