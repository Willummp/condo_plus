package com.condoplus.notificacao.canal;

import java.time.LocalDateTime;

public sealed interface ResultadoEnvio {
    LocalDateTime momento();

    record Sucesso(LocalDateTime momento, String referencia) implements ResultadoEnvio {}
    record Falha(LocalDateTime momento, String mensagem, boolean retentavel) implements ResultadoEnvio {}
}
