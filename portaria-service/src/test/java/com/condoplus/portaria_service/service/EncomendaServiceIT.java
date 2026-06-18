package com.condoplus.portaria_service.service;

import com.condoplus.portaria_service.dto.response.EncomendaResponseDTO;
import com.condoplus.portaria_service.dto.request.NovaEncomendaRequest;
import com.condoplus.portaria_service.dto.request.RetiradaRequest;
import com.condoplus.portaria_service.model.enums.StatusEncomenda;
import com.condoplus.portaria_service.model.enums.TipoEncomenda;
import com.condoplus.portaria_service.repository.EncomendaRepository;
import com.redis.testcontainers.RedisContainer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class EncomendaServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static RedisContainer redis =
            new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // TTL reduzido para 1 minuto — evita esperar 2h no teste de expiração
        registry.add("condoplus.portaria.encomendas.curto-prazo-ttl-minutos", () -> "1");
    }

    @Autowired private EncomendaService encomendaService;
    @Autowired private EncomendaRedisStore redisStore;
    @Autowired private EncomendaRepository encomendaRepository;

    private UUID unidadeId;
    private UUID porteiroId;

    @BeforeEach
    void setUp() {
        unidadeId = UUID.randomUUID();
        porteiroId = UUID.randomUUID();
        encomendaRepository.deleteAll();
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 1: CURTO_PRAZO recebida cria chave no Redis
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Encomenda CURTO_PRAZO recebida deve criar chave no Redis")
    void curtoPrazoRecebidaDeveCriarChaveNoRedis() {
        var req = new NovaEncomendaRequest(
                unidadeId, TipoEncomenda.CURTO_PRAZO, "iFood pizza", null);

        EncomendaResponseDTO resp = encomendaService.receber(req, porteiroId);

        assertThat(redisStore.estaAtiva(unidadeId, resp.id())).isTrue();
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 2: MEDIO_PRAZO não vai para o Redis
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Encomenda MEDIO_PRAZO recebida NÃO deve criar chave no Redis")
    void medioPrazoNaoDeveIrParaRedis() {
        var req = new NovaEncomendaRequest(
                unidadeId, TipoEncomenda.MEDIO_PRAZO, "Caixa Amazon", "TRACK123");

        EncomendaResponseDTO resp = encomendaService.receber(req, porteiroId);

        assertThat(redisStore.estaAtiva(unidadeId, resp.id())).isFalse();
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 3: LONGO_PRAZO não vai para o Redis
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Encomenda LONGO_PRAZO recebida NÃO deve criar chave no Redis")
    void longoPrazoNaoDeveIrParaRedis() {
        var req = new NovaEncomendaRequest(
                unidadeId, TipoEncomenda.LONGO_PRAZO, "Importação Shopee", null);

        EncomendaResponseDTO resp = encomendaService.receber(req, porteiroId);

        assertThat(redisStore.estaAtiva(unidadeId, resp.id())).isFalse();
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 4: retirada remove chave do Redis e atualiza status no banco
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Retirada de CURTO_PRAZO deve remover chave do Redis e marcar RETIRADA no banco")
    void retiradaDeveRemoverChaveDoRedis() {
        var req = new NovaEncomendaRequest(
                unidadeId, TipoEncomenda.CURTO_PRAZO, "iFood pizza", null);

        EncomendaResponseDTO criada = encomendaService.receber(req, porteiroId);
        assertThat(redisStore.estaAtiva(unidadeId, criada.id())).isTrue();

        encomendaService.retirar(
                criada.id(),
                new RetiradaRequest(UUID.randomUUID()),
                porteiroId);

        assertThat(redisStore.estaAtiva(unidadeId, criada.id())).isFalse();
        assertThat(encomendaRepository.findById(criada.id()))
                .isPresent()
                .get()
                .extracting(e -> e.getStatus())
                .isEqualTo(StatusEncomenda.RETIRADA);
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 5: chave expira automaticamente após TTL
    // TTL = 1 minuto via @DynamicPropertySource — timeout de 2 min com folga
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Chave Redis de CURTO_PRAZO deve expirar automaticamente após TTL")
    void chaveDeveExpirarAposTtl() {
        var req = new NovaEncomendaRequest(
                unidadeId, TipoEncomenda.CURTO_PRAZO, "Pizza teste TTL", null);

        EncomendaResponseDTO criada = encomendaService.receber(req, porteiroId);
        assertThat(redisStore.estaAtiva(unidadeId, criada.id())).isTrue();

        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        assertThat(redisStore.estaAtiva(unidadeId, criada.id()))
                                .isFalse()
                );
    }
}