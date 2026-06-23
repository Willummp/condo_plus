package com.condoplus.notificacao.service;

import com.condoplus.notificacao.domain.TipoEvento;
import java.util.Map;
import java.util.UUID;

public record EventoNotificacao(
        String eventoOrigemId,
        TipoEvento tipoEvento,
        String titulo,
        String corpo,
        UUID unidadeId,
        UUID pessoaIdEspecifica,
        Map<String, String> metados
) {}
