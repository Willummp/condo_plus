package com.condoplus.notificacao.dto;

import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.TipoEvento;
import java.util.UUID;

public record AtualizarPreferenciaRequest(
        UUID pessoaId,
        TipoEvento tipoEvento,
        Canal canal,
        boolean ativa
) {}
