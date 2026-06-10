package com.condoplus.condominio.estrutura.dto;

import com.condoplus.condominio.estrutura.domain.TipoVinculacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO (Record) de entrada contendo os parâmetros necessários para associar uma pessoa física a uma unidade residencial.
 * 
 * @param pessoaId O ID exclusivo (UUID) da pessoa física a ser vinculada. Não pode ser nulo.
 * @param tipo O tipo do relacionamento ou vínculo de moradia (ex: PROPRIETARIO, RESIDENTE). Não pode ser nulo.
 * @param dataInicio A data de início da vigência do vínculo residencial. Não pode ser nula e deve ser contemporânea (passado ou presente).
 */
public record NovaVinculacaoRequest(
    @NotNull UUID pessoaId,
    @NotNull TipoVinculacao tipo,
    @NotNull @PastOrPresent LocalDate dataInicio
) {}
