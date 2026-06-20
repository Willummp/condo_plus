package com.condoplus.condominio.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro Servlet para interceptação de requisições no condominio-service.
 * 
 * <p>Responsável por capturar o cabeçalho {@code X-Correlation-ID} enviado pelo API Gateway
 * e registrá-lo no Contexto de Diagnóstico Mapeado (MDC) do Logback/SLF4J.
 * 
 * <p>Assim, qualquer log emitido na thread desta requisição conterá automaticamente o 
 * identificador de correlação para fins de rastreamento.
 */
@Component
public class CorrelationFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest) {
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);

            // Se a requisição não passou pelo gateway (ex: chamada direta local), gera um ID novo
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }

            try {
                MDC.put(MDC_CORRELATION_ID_KEY, correlationId);
                chain.doFilter(request, response);
            } finally {
                // Remove o ID para evitar memory leaks na reciclagem das threads do pool do Tomcat
                MDC.remove(MDC_CORRELATION_ID_KEY);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
