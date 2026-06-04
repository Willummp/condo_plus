package com.condoplus.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe de inicialização (Bootstrap) do Servidor de Gateway da API (Spring Cloud Gateway).
 * 
 * <p>Este componente atua como a **Borda (Edge Service)** e porta de entrada unificada de toda a 
 * infraestrutura Condo Plus. Executa na porta **8080** e gerencia o tráfego externo realizando:
 * <ul>
 *   <li>Roteamento dinâmico inteligente integrado com o Eureka Service Discovery.</li>
 *   <li>Load Balancing (balanceamento de carga) inteligente entre as instâncias ativas do mesmo microsserviço.</li>
 *   <li>Filtros globais para propagação segura de cabeçalhos de contexto e tokens de autenticação.</li>
 * </ul>
 * 
 * <p>Anotações e conceitos aplicados:
 * <ul>
 *   <li>{@code @SpringBootApplication} — Declara a classe como ponto de entrada Spring Boot, carregando todas as configurações de rotas baseadas no YAML.</li>
 * </ul>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    /**
     * Ponto de entrada padrão da aplicação Java (Main method) que inicializa o API Gateway.
     * 
     * @param args Parâmetros opcionais de linha de comando.
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
