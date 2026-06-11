package com.condoplus.auditoria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Aplicacao principal do auditoria-service.
 *
 * Responsabilidades:
 * - Coletar eventos auditaveis do sistema (TP1: via REST direto; TP2: via Kafka)
 * - Persistir em MongoDB de forma idempotente
 * - Expor endpoints REST de consulta com filtros, paginacao e agregacao
 *
 * Por que MongoDB e nao PostgreSQL?
 * - Payload heterogeneo por tipo de evento
 * - Perfil write-heavy com poucas updates
 * - Queries naturalmente time-series
 * - TTL nativo para retencao automatica
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoAuditing
public class AuditoriaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditoriaServiceApplication.class, args);
    }
}