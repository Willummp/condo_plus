package com.condoplus.condominio.estrutura.dto;

import java.util.Set;

public record CriarCredencialRequest(
    String email,
    String senha,
    Set<String> roles
) {}
