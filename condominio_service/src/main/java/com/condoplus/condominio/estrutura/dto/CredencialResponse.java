package com.condoplus.condominio.estrutura.dto;

import java.util.UUID;

public record CredencialResponse(
    UUID id,
    String email,
    String role
) {}
