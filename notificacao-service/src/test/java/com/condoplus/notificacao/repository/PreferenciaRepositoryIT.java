package com.condoplus.notificacao.repository;

import com.condoplus.notificacao.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import java.util.UUID;

@DataR2dbcTest
@Testcontainers
class PreferenciaRepositoryIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort()
                        + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired PreferenciaRepository preferenciaRepository;

    @Test
    void salvaERecupera() {
        UUID pessoaId = UUID.randomUUID();
        PreferenciaNotificacao p = new PreferenciaNotificacao();
        p.setPessoaId(pessoaId);
        p.setTipoEvento(TipoEvento.MULTA_APLICADA);
        p.setCanal(Canal.EMAIL);
        p.setAtiva(true);

        StepVerifier.create(preferenciaRepository.save(p))
                .assertNext(salva -> {
                    assert salva.getId() != null;
                    assert salva.getPessoaId().equals(pessoaId);
                })
                .verifyComplete();
    }
}
