package com.condoplus.portaria_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public void salvarComTTL(String key, Duration ttl) {
        redisTemplate.opsForValue().set(key, "1", ttl);
    }

    public boolean existe(String key) {
        return redisTemplate.hasKey(key);
    }

    public void deletar(String key) {
        redisTemplate.delete(key);
    }
}
