package com.condoplus.condominio.convivencia.dto;

import com.condoplus.condominio.convivencia.domain.PublicoAlvo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NovoComunicadoRequest(
    @NotBlank
    @Size(max = 150, message = "O título do comunicado não pode exceder 150 caracteres")
    String titulo,

    @NotBlank
    String conteudo,

    @NotNull(message = "A visibilidade é obrigatória")
    PublicoAlvo visibilidade
) {}
