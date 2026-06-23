package com.condoplus.notificacao.dto;

import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.TipoEvento;
import java.util.UUID;

public record NotificacaoRequest(
        UUID pessoaId,
        String eventoOrigemId,
        TipoEvento tipoEvento,
        Canal canal,
        String mensagem
) {}
