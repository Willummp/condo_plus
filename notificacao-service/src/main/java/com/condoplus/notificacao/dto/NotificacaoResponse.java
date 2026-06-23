package com.condoplus.notificacao.dto;

import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.StatusNotificacao;
import com.condoplus.notificacao.domain.TipoEvento;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacaoResponse(
        UUID id,
        UUID pessoaId,
        TipoEvento tipoEvento,
        String eventoOrigemId,
        Canal canal,
        String titulo,
        String corpo,
        StatusNotificacao status,
        int tentativas,
        LocalDateTime criadaEm
) {}
