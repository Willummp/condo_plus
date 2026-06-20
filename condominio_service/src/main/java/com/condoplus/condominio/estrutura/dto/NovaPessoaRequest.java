package com.condoplus.condominio.estrutura.dto;

import jakarta.validation.constraints.*;

/**
 * DTO (Record) de entrada contendo os parâmetros necessários para cadastrar uma nova pessoa física e criar suas credenciais de segurança.
 * 
 * @param nomeCompleto O nome completo da pessoa física. Não pode estar em branco e possui limite máximo de 200 caracteres.
 * @param email Endereço de e-mail de login para criação de conta de acesso. Não pode estar em branco e deve ser um e-mail formalmente válido.
 * @param senhaInicial A senha de acesso inicial para o condomínio. Deve conter entre 8 e 128 caracteres.
 * @param documento O CPF de identificação nacional (contendo apenas dígitos). Não pode estar em branco e deve ter entre 11 e 14 caracteres.
 * @param telefone O número de telefone de contato opcional.
 * @param emailContato Endereço eletrônico preferencial opcional para comunicações operacionais.
 * @param role O papel inicial atribuído à credencial (ex: SINDICO, MORADOR, FUNCIONARIO). Não pode estar em branco.
 */
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
