package com.condoplus.portaria_service.dto.client;

import java.util.UUID;

public record PessoaExterno(
        UUID id,
        UUID credencialId,
        String nomeCompleto,
        String documento,
        String telefone,
        String emailContato
) {}
