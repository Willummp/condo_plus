package com.condoplus.portaria_service.service;

import com.condoplus.portaria_service.config.properties.PortariaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EncomendaRedisStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final PortariaProperties properties;

    private String chave(UUID unidadeId, UUID encomendaId) {
        return "encomenda:curto:" + unidadeId + ":" + encomendaId;
    }

    public void registrarCurtoPrazo(UUID unidadeId, UUID encomendaId) {
        String chave = chave(unidadeId, encomendaId);
        Duration ttl = Duration.ofMinutes(properties.encomendas().curtoPrazoTtlMinutos());
        redisTemplate.opsForValue().set(chave, encomendaId.toString(), ttl);
        log.debug("Encomenda CURTO_PRAZO registrada no Redis. chave={} ttlMin={}",
                chave, ttl.toMinutes());
    }

    public void removerCurtoPrazo(UUID unidadeId, UUID encomendaId) {
        String chave = chave(unidadeId, encomendaId);
        Boolean removed = redisTemplate.delete(chave);
        log.debug("Encomenda removida do Redis. chave={} removed={}", chave, removed);
    }

    public boolean estaAtiva(UUID unidadeId, UUID encomendaId) {
        String chave = chave(unidadeId, encomendaId);
        return redisTemplate.hasKey(chave);
    }
}
