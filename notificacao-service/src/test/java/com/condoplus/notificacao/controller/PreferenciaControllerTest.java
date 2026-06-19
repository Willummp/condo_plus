package com.condoplus.notificacao.controller;

import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.PreferenciaNotificacao;
import com.condoplus.notificacao.domain.TipoEvento;
import com.condoplus.notificacao.dto.AtualizarPreferenciaRequest;
import com.condoplus.notificacao.service.PreferenciaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(PreferenciaController.class)
class PreferenciaControllerTest {
    @Autowired private WebTestClient webTestClient;
    @MockBean private PreferenciaService preferenciaService;

    @Test
    void actualizarPreferenciaRetorna200() {
        UUID pessoaId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AtualizarPreferenciaRequest req = new AtualizarPreferenciaRequest(
                pessoaId, TipoEvento.MULTA_APLICADA, Canal.EMAIL, true);

        PreferenciaNotificacao pref = new PreferenciaNotificacao();
        pref.setPessoaId(pessoaId);
        pref.setTipoEvento(TipoEvento.MULTA_APLICADA);
        pref.setCanal(Canal.EMAIL);
        pref.setAtiva(true);

        when(preferenciaService.atualizar(eq(pessoaId), any()))
                .thenReturn(Mono.just(pref));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                pessoaId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth))
                .put()
                .uri("/notificacoes/preferencias")
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk();
    }
}
