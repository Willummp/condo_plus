package com.condoplus.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Classe de inicialização (Bootstrap) do Servidor de Registro e Descoberta de Serviços (Netflix Eureka Server).
 * 
 * <p>Este componente central atua como o **Service Registry** de toda a arquitetura de microsserviços do 
 * ecossistema Condo Plus. Todos os microsserviços (como o `condominio-service` e o `iam-service`) se registram 
 * dinamicamente nesta instância informando seus nomes lógicos e endereços de rede, eliminando a necessidade 
 * de configurações estáticas de IPs e portas (Hardcoded Configs).
 * 
 * <p>Por padrão de arquitetura estabelecido, este servidor executa na porta **8761**.
 * 
 * <p>Anotações e conceitos aplicados:
 * <ul>
 *   <li>{@code @SpringBootApplication} — Declara esta classe como o ponto de entrada de uma aplicação Spring Boot, ativando a auto-configuração e a varredura de componentes.</li>
 *   <li>{@code @EnableEurekaServer} — Ativa o mecanismo de Service Registry do Netflix Eureka Server dentro do contexto do Spring Cloud.</li>
 * </ul>
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    /**
     * Ponto de entrada padrão da aplicação Java (Main method) que inicializa o Eureka Server.
     * 
     * @param args Parâmetros opcionais de linha de comando.
     */
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
