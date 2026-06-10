package com.condoplus.condominio.convivencia.domain;

import com.condoplus.condominio.estrutura.domain.Pessoa;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa um Comunicado publicado pela administração ou síndico no condomínio.
 * 
 * <p>Anotações da classe:
 * <ul>
 *   <li>{@code @Table(schema = "condominio", value = "comunicado")} — Mapeia a classe para a tabela {@code comunicado} do schema {@code condominio}.</li>
 *   <li>{@code @Getter} — Gera os métodos getters pelo Lombok.</li>
 *   <li>{@code @Setter} — Gera os métodos setters pelo Lombok.</li>
 *   <li>{@code @NoArgsConstructor} — Gera o construtor padrão vazio pelo Lombok.</li>
 * </ul>
 */
@Table(schema = "condominio", value = "comunicado")
@Getter
@Setter
@NoArgsConstructor
public class Comunicado {

    @Id
    private UUID id;

    @Column("titulo")
    private String titulo;

    @Column("mensagem")
    private String mensagem;

    @Column("data_publicacao")
    private LocalDateTime dataPublicacao;

    @Column("autor_id")
    private AggregateReference<Pessoa, UUID> autorId;

    @Column("publico_alvo")
    private PublicoAlvo publicoAlvo;

    /**
     * Bloco residencial de destino. 
     * Obrigatório apenas quando {@code publicoAlvo} é classificado como {@code PublicoAlvo.BLOCO_ESPECIFICO}.
     */
    @Column("bloco_alvo")
    private String blocoAlvo;
}
