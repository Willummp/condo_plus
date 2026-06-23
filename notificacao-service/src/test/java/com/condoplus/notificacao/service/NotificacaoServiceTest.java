package com.condoplus.notificacao.service;

import com.condoplus.notificacao.domain.*;
import com.condoplus.notificacao.repository.NotificacaoRepository;
import com.condoplus.notificacao.repository.PreferenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {
    @Mock private NotificacaoRepository notificacaoRepository;
    @Mock private PreferenciaRepository preferenciaRepository;
    @Mock private ResolverDestinatariosService resolverService;
    @Mock private DespacharService despacharService;
    @InjectMocks private NotificacaoService notificacaoService;

    @Test
    void processarEventoComDoisDestinatariosEDoisCanaisProduzQuatroNotificacoes() {
        UUID dest1 = UUID.randomUUID();
        UUID dest2 = UUID.randomUUID();
        EventoNotificacao evento = new EventoNotificacao(
                "evt-123", TipoEvento.COMUNICADO_PUBLICADO,
                "Aviso", "Corpo do aviso", null, null, null);

        when(resolverService.resolverDestinatarios(evento))
                .thenReturn(Flux.just(dest1, dest2));

        when(preferenciaRepository
                .findByPessoaIdAndTipoEventoAndAtivaTrue(any(), any()))
                .thenReturn(Flux.just(
                        preferencia(Canal.EMAIL),
                        preferencia(Canal.PUSH)));

        when(notificacaoRepository.save(any()))
                .thenAnswer(inv -> Mono.just((Notificacao) inv.getArgument(0)));

        when(despacharService.despachar(any()))
                .thenAnswer(inv -> Mono.just((Notificacao) inv.getArgument(0)));

        StepVerifier.create(notificacaoService.processarEvento(evento))
                .expectNextCount(4)
                .verifyComplete();
    }

    private PreferenciaNotificacao preferencia(Canal canal) {
        PreferenciaNotificacao pref = new PreferenciaNotificacao();
        pref.setPessoaId(UUID.randomUUID());
        pref.setTipoEvento(TipoEvento.COMUNICADO_PUBLICADO);
        pref.setCanal(canal);
        pref.setAtiva(true);
        return pref;
    }
}
