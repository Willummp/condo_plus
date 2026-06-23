package com.condoplus.condominio.convivencia.dto;

import com.condoplus.condominio.convivencia.domain.PublicoAlvo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO (Data Transfer Object) de entrada que representa uma solicitação de publicação de novo Comunicado.
 * 
 * <p>Anotações de validação dos campos:
 * <ul>
 *   <li>{@code @NotBlank} — Garante que a String informada não seja nula, vazia ou contenha apenas espaços em branco.</li>
 *   <li>{@code @Size} — Restringe o comprimento da String informada, definindo limites mínimos ou máximos de caracteres.</li>
 *   <li>{@code @NotNull} — Exige o preenchimento do campo enumerado visibilidade.</li>
 * </ul>
 */
public record NovoComunicadoRequest(
    @NotBlank
    @Size(max = 150, message = "O título do comunicado não pode exceder 150 caracteres")
    String titulo,

    @NotBlank
    String conteudo,

    @NotNull(message = "A visibilidade é obrigatória")
    PublicoAlvo visibilidade
) {}
