package com.condoplus.condominio.estrutura.dto;

public record CriarCredencialRequest(
    String email,
    String senha,
    String role
) {}
