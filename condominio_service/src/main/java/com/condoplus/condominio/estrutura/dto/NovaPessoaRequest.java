package com.condoplus.condominio.estrutura.dto;

import jakarta.validation.constraints.*;

public record NovaPessoaRequest(
    @NotBlank @Size(max = 200) String nomeCompleto,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 128) String senhaInicial,
    @NotBlank @Size(min = 11, max = 14) @Pattern(regexp = "[0-9]+", message = "Documento deve conter apenas dígitos")
    String documento,
    String telefone,
    @Email String emailContato,
    @NotBlank String role
) {}
