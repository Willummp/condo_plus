package com.condoplus.portaria_service.dto;

import com.condoplus.portaria_service.model.enums.TipoVisitante;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.UUID;

public record CriarVisitanteDTO(

        @NotBlank(message = "nome é obrigatório")
        @Size(max = 200, message = "nome deve ter no máximo 200 caracteres")
        String nome,

        @NotBlank(message = "documento é obrigatório")
        @Size(max = 20, message = "documento deve ter no máximo 20 caracteres")
        @Pattern(regexp = "^[0-9A-Za-z.\\-/]+$", message = "documento com formato inválido")
        String documento,

        @Size(max = 20, message = "telefone deve ter no máximo 20 caracteres")
        String telefone,

        @NotNull(message = "tipo é obrigatório")
        TipoVisitante tipo,

        @NotNull(message = "autorizadoPorPessoaId é obrigatório")
        UUID autorizadoPorPessoaId,

        @NotNull(message = "autorizadoParaUnidadeId é obrigatório")
        UUID autorizadoParaUnidadeId,

        @NotNull(message = "validadeInicio é obrigatório")
        @FutureOrPresent(message = "validadeInicio não pode ser no passado")
        LocalDateTime validadeInicio,

        @NotNull(message = "validadeFim é obrigatório")
        @Future(message = "validadeFim deve ser uma data futura")
        LocalDateTime validadeFim

) {}