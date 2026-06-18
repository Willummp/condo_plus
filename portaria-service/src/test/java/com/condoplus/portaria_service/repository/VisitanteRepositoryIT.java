package com.condoplus.portaria_service.repository;

import com.condoplus.portaria_service.model.entities.Visitante;
import com.condoplus.portaria_service.model.enums.StatusVisitante;
import com.condoplus.portaria_service.model.enums.TipoVisitante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class VisitanteRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private VisitanteRepository visitanteRepository;

    // ─────────────────────────────────────────────────────────
    // Cenário 1: visitante dentro da janela de validade é encontrado
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAtivosPorDocumento retorna visitante dentro da janela de validade")
    void deveEncontrarVisitanteValido() {
        Visitante v = Visitante.builder()
                .nome("João Visitante")
                .documento("12345678901")
                .tipo(TipoVisitante.SOCIAL)
                .autorizadoPorPessoaId(UUID.randomUUID())
                .autorizadoParaUnidadeId(UUID.randomUUID())
                .validadeInicio(LocalDateTime.now().minusHours(1))
                .validadeFim(LocalDateTime.now().plusHours(2))
                .status(StatusVisitante.AUTORIZADO)
                .build();

        visitanteRepository.save(v);

        List<Visitante> ativos = visitanteRepository.findAtivosPorDocumento(
                "12345678901", StatusVisitante.AUTORIZADO, LocalDateTime.now());

        assertThat(ativos).hasSize(1);
        assertThat(ativos.get(0).getNome()).isEqualTo("João Visitante");
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 2: visitante com validade vencida não aparece
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAtivosPorDocumento não retorna visitante com validade vencida")
    void naoDeveRetornarVisitanteVencido() {
        Visitante v = Visitante.builder()
                .nome("Maria Vencida")
                .documento("98765432100")
                .tipo(TipoVisitante.SOCIAL)
                .autorizadoPorPessoaId(UUID.randomUUID())
                .autorizadoParaUnidadeId(UUID.randomUUID())
                .validadeInicio(LocalDateTime.now().minusDays(3))
                .validadeFim(LocalDateTime.now().minusDays(1))
                .status(StatusVisitante.AUTORIZADO)
                .build();

        visitanteRepository.save(v);

        List<Visitante> ativos = visitanteRepository.findAtivosPorDocumento(
                "98765432100", StatusVisitante.AUTORIZADO, LocalDateTime.now());

        assertThat(ativos).isEmpty();
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 3: visitante BLOQUEADO não aparece mesmo dentro da janela
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAtivosPorDocumento não retorna visitante BLOQUEADO")
    void naoDeveRetornarVisitanteBloqueado() {
        Visitante v = Visitante.builder()
                .nome("Carlos Bloqueado")
                .documento("11122233300")
                .tipo(TipoVisitante.SOCIAL)
                .autorizadoPorPessoaId(UUID.randomUUID())
                .autorizadoParaUnidadeId(UUID.randomUUID())
                .validadeInicio(LocalDateTime.now().minusHours(1))
                .validadeFim(LocalDateTime.now().plusHours(2))
                .status(StatusVisitante.BLOQUEADO)
                .build();

        visitanteRepository.save(v);

        List<Visitante> ativos = visitanteRepository.findAtivosPorDocumento(
                "11122233300", StatusVisitante.AUTORIZADO, LocalDateTime.now());

        assertThat(ativos).isEmpty();
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 4: documento inexistente retorna lista vazia
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAtivosPorDocumento retorna vazio para documento inexistente")
    void deveRetornarVazioParaDocumentoInexistente() {
        List<Visitante> ativos = visitanteRepository.findAtivosPorDocumento(
                "00000000000", StatusVisitante.AUTORIZADO, LocalDateTime.now());

        assertThat(ativos).isEmpty();
    }
}