package com.condoplus.portaria_service.service;

import com.condoplus.portaria_service.dto.request.NovoVisitanteRequest;
import com.condoplus.portaria_service.dto.response.VisitanteResponseDTO;
import com.condoplus.portaria_service.exception.AutorizacaoNegadaException;
import com.condoplus.portaria_service.model.enums.TipoVisitante;
import com.condoplus.portaria_service.repository.VisitanteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitanteServiceTest {

    @Mock
    private VisitanteRepository visitanteRepository;

    private VisitanteService service;

    @BeforeEach
    void setUp() {
        service = new VisitanteService(visitanteRepository);
    }

    /**
     * Limpa o SecurityContext após cada teste para evitar
     * contaminação de identidade entre casos.
     */
    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 1: SÍNDICO autoriza PRESTADOR → sucesso
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("SÍNDICO pode autorizar visitante do tipo PRESTADOR")
    void prestadorAutorizadoPorSindicoDeveFuncionar() {
        autenticarComo("ROLE_SINDICO");
        when(visitanteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VisitanteResponseDTO resp = service.autorizar(umPrestador(), UUID.randomUUID());

        assertThat(resp.tipo()).isEqualTo(TipoVisitante.PRESTADOR);
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 2: ADMIN também pode autorizar PRESTADOR
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN pode autorizar visitante do tipo PRESTADOR")
    void prestadorAutorizadoPorAdminDeveFuncionar() {
        autenticarComo("ROLE_ADMIN");
        when(visitanteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VisitanteResponseDTO resp = service.autorizar(umPrestador(), UUID.randomUUID());

        assertThat(resp.tipo()).isEqualTo(TipoVisitante.PRESTADOR);
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 3: morador comum tenta autorizar PRESTADOR → 403
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Morador comum NÃO pode autorizar visitante do tipo PRESTADOR")
    void prestadorAutorizadoPorMoradorDeveFalhar() {
        autenticarComo("ROLE_MORADOR");

        assertThatThrownBy(() -> service.autorizar(umPrestador(), UUID.randomUUID()))
                .isInstanceOf(AutorizacaoNegadaException.class)
                .hasMessageContaining("PRESTADOR");
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 4: morador comum autoriza SOCIAL → sucesso
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Morador comum pode autorizar visitante do tipo SOCIAL")
    void socialAutorizadoPorMoradorDeveFuncionar() {
        autenticarComo("ROLE_MORADOR");
        when(visitanteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VisitanteResponseDTO resp = service.autorizar(umSocial(), UUID.randomUUID());

        assertThat(resp.tipo()).isEqualTo(TipoVisitante.SOCIAL);
    }

    // ─────────────────────────────────────────────────────────
    // Cenário 5: sem autenticação tenta autorizar PRESTADOR → 403
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Request sem autenticação NÃO pode autorizar PRESTADOR")
    void prestadorSemAutenticacaoDeveFalhar() {
        // SecurityContext vazio — nenhum token presente
        assertThatThrownBy(() -> service.autorizar(umPrestador(), UUID.randomUUID()))
                .isInstanceOf(AutorizacaoNegadaException.class);
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private void autenticarComo(String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(), null,
                List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private NovoVisitanteRequest umPrestador() {
        return new NovoVisitanteRequest(
                "João Encanador",
                "12345678901",
                "21999999999",
                TipoVisitante.PRESTADOR,
                UUID.randomUUID(),
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3)
        );
    }

    private NovoVisitanteRequest umSocial() {
        return new NovoVisitanteRequest(
                "Maria Convidada",
                "98765432100",
                "21988888888",
                TipoVisitante.SOCIAL,
                UUID.randomUUID(),
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3)
        );
    }
}