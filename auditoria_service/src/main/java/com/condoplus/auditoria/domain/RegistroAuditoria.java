package com.condoplus.auditoria.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

/**
 * Documento principal do auditoria-service.
 *
 * Representa UM evento auditavel ocorrido em qualquer servico do Condo+.
 *
 * Justificativa da escolha do MongoDB:
 * 1. Payload heterogeneo: cada tipo de evento tem campos diferentes.
 *    MULTA_APLICADA tem valor/motivo; ENCOMENDA_RECEBIDA tem unidade/tipo;
 *    LOGIN_FALHADO tem ip/userAgent. Mapeamento documental absorve isso
 *    sem explodir em tabelas (alternativa relacional) ou perder garantias
 *    (coluna JSON em PostgreSQL).
 * 2. Write-heavy com pouco update: insere muito, atualiza quase nunca.
 *    Unique index absorve inserts com throughput alto. Sem transacao
 *    multi-documento — cada evento e independente.
 * 3. Queries time-series: "eventos do dia X", "historico da entidade Y".
 *    CompoundIndex em (entidade.tipo, entidade.id, timestamp DESC)
 *    acelera padrao tipico de consulta.
 * 4. TTL nativo: MongoDB apaga registros antigos automaticamente via
 *    expireAfterSeconds — sem job/cron externo. Em PostgreSQL exigiria
 *    pg_partman + particionamento + job de drop, muito mais complexo.
 *
 * Cobre o registro de eventos, a persistencia nao-relacional e a rastreabilidade via correlationId.
 */
@Document(collection = "registros_auditoria")
@CompoundIndexes({
        // Acelera "historico de uma entidade em ordem cronologica reversa"
        @CompoundIndex(
                name = "idx_entidade_timestamp",
                def = "{'entidadeAfetada.tipo': 1, 'entidadeAfetada.id': 1, 'timestamp': -1}"
        ),
        // Acelera "todos os eventos de um servico nas ultimas 24h"
        @CompoundIndex(
                name = "idx_servico_timestamp",
                def = "{'servicoOrigem': 1, 'timestamp': -1}"
        )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAuditoria {

    @Id
    private String id; // ObjectId gerado pelo MongoDB, convertido para String

    /**
     * Identificador unico do evento de origem.
     *
     * @Indexed(unique = true) e o mecanismo de IDEMPOTENCIA do servico.
     * Se Kafka redelivera no TP2 (at-least-once delivery), o segundo save
     * lanca DuplicateKeyException — capturando e tratando como sucesso
     * silencioso. Sem isso, redelivery duplica registros e polui o banco.
     */
    @Indexed(unique = true)
    @Field("eventId")
    private String eventId;

    /**
     * Identificador de correlacao distribuida.
     * Propagado pelo Gateway via header X-Correlation-Id em todas as chamadas.
     * Permite rastrear uma operacao que atravessa multiplos servicos.
     */
    @Indexed
    @Field("correlationId")
    private String correlationId;

    /**
     * Quando o evento ocorreu (no servico de origem, nao quando chegou aqui).
     *
     * @Indexed com expireAfterSeconds = 365 dias = TTL nativo do MongoDB.
     * Thread interna do servidor apaga documentos antigos automaticamente.
     * Em produccao real, esse valor sairia de application-observability.yml.
     */
    @Indexed(name = "idx_timestamp_ttl", expireAfterSeconds = 31_536_000)
    @Field("timestamp")
    private Instant timestamp;

    @Indexed
    @Field("tipoEvento")
    private TipoEvento tipoEvento;

    @Indexed
    @Field("servicoOrigem")
    private String servicoOrigem; // "iam-service", "condominio-service", etc.

    @Field("pessoaIniciadora")
    private PessoaIniciadora pessoaIniciadora; // pode ser null para eventos de sistema

    @Field("entidadeAfetada")
    private EntidadeAfetada entidadeAfetada;

    /**
     * Payload completo do evento de origem.
     *
     * Map aberto porque cada tipo de evento tem campos diferentes. Em
     * PostgreSQL exigiria tabela por tipo (explosao) ou coluna JSON (perde
     * garantias do relacional). MongoDB indexa internamente se necessario
     * (ex: db.registros_auditoria.createIndex({"payload.unidadeId": 1})).
     */
    @Field("payload")
    private Map<String, Object> payload;

    /**
     * Metadados tecnicos do evento (IP, user-agent, host, etc).
     * Separados do payload para nao poluir os campos de negocio.
     */
    @Field("metadados")
    private Map<String, String> metadados;

    /**
     * Auditoria automatica: quando o registro foi inserido no MongoDB.
     * Diferente de 'timestamp' (que e quando o evento ocorreu na origem).
     * Util para medir latencia entre origem e auditoria (TP2: observabilidade).
     */
    @CreatedDate
    @Field("dataInsercao")
    private Instant dataInsercao;
}