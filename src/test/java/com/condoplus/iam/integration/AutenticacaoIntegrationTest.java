package com.condoplus.iam.integration;

import com.condoplus.iam.domain.CredencialUsuario;
import com.condoplus.iam.domain.Role;
import com.condoplus.iam.domain.StatusCredencial;
import com.condoplus.iam.domain.TipoRole;
import com.condoplus.iam.dto.LoginRequest;
import com.condoplus.iam.repository.CredencialRepository;
import com.condoplus.iam.repository.RoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AutenticacaoIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("condoplus_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registrarPropriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.schemas", () -> "public");
        registry.add(
                "spring.jpa.properties.hibernate.default_schema",
                () -> "public"
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CredencialRepository credencialRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void limparEPreparar() {
        credencialRepository.deleteAll();

        Role moradorRole = roleRepository.findByNome(TipoRole.MORADOR)
                .orElseThrow();

        CredencialUsuario cred = CredencialUsuario.builder()
                .email("morador@condo.com")
                .senhaHash(passwordEncoder.encode("senha123"))
                .status(StatusCredencial.ATIVO)
                .roles(Set.of(moradorRole))
                .build();

        credencialRepository.save(cred);
    }

    @Test
    void fluxoCompletoDeLogin() throws Exception {
        LoginRequest req = new LoginRequest(
                "morador@condo.com",
                "senha123"
        );

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresInSeconds").exists());
    }
}