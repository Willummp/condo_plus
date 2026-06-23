package com.condoplus.condominio.estrutura.repository;

import com.condoplus.condominio.estrutura.domain.TipoUnidade;
import com.condoplus.condominio.estrutura.domain.Unidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJdbcTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UnidadeRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("condoplus")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "?currentSchema=condominio");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Test
    @DisplayName("Deve persistir e recuperar uma Unidade do banco de dados PostgreSQL real usando Testcontainers")
    void persistirERecuperarUnidade() {

        Unidade unidade = Unidade.criar("304", "Bloco C", TipoUnidade.APARTAMENTO);

        Unidade salva = unidadeRepository.save(unidade);
        assertNotNull(salva.getId());

        Optional<Unidade> encontrada = unidadeRepository.findById(salva.getId());

        assertTrue(encontrada.isPresent());
        assertEquals("304", encontrada.get().getNumero());
        assertEquals("Bloco C", encontrada.get().getBloco());
        assertEquals(TipoUnidade.APARTAMENTO, encontrada.get().getTipo());
        assertTrue(encontrada.get().isAtiva());
    }

    @Test
    @DisplayName("Deve retornar true se a unidade existe por número e bloco")
    void existsByNumeroAndBloco() {

        Unidade unidade = Unidade.criar("102", "Bloco B", TipoUnidade.APARTAMENTO);
        unidadeRepository.save(unidade);

        assertTrue(unidadeRepository.existsByNumeroAndBloco("102", "Bloco B"));
        assertFalse(unidadeRepository.existsByNumeroAndBloco("102", "Bloco A"));
    }
}
