package com.condoplus.condominio.estrutura.service;

import com.condoplus.condominio.estrutura.domain.*;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

/**
 * Serviço especializado responsável por calcular e atualizar os escopos das vinculações das unidades.
 * 
 * <p>Esta é a invariante mais importante do bounded context de Estrutura.
 * Os escopos (SOCIAL, LEGAL, FINANCEIRO) determinam quem tem voz e privilégios de decisão sobre cada unidade.
 * 
 * <p>Regras de Derivação de Escopos (Seção 4.4 do Documento de Projeto Condo Plus):
 * <table>
 *   <caption>Matriz de Escopos e privilégios</caption>
 *   <thead>
 *     <tr>
 *       <th>Situação do Vínculo</th>
 *       <th>SOCIAL (Cotidiano/Reservas)</th>
 *       <th>LEGAL (Votação/Assembleia)</th>
 *       <th>FINANCEIRO (Boletos/Taxas)</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>Dono Residindo ({@code PROPRIETARIO_RESIDENTE})</td>
 *       <td>Sim (✓)</td>
 *       <td>Sim (✓)</td>
 *       <td>Sim (✓)</td>
 *     </tr>
 *     <tr>
 *       <td>Só Inquilino ({@code RESIDENTE})</td>
 *       <td>Sim (✓)</td>
 *       <td>Não (✗)</td>
 *       <td>Não (✗)</td>
 *     </tr>
 *     <tr>
 *       <td>Dono com Apto Alugado ({@code PROPRIETARIO} c/ residente)</td>
 *       <td>Não (✗)</td>
 *       <td>Sim (✓)</td>
 *       <td>Sim (✓)</td>
 *     </tr>
 *     <tr>
 *       <td>Dono com Apto Vazio ({@code PROPRIETARIO} s/ residente)</td>
 *       <td>Sim (✓)</td>
 *       <td>Sim (✓)</td>
 *       <td>Sim (✓)</td>
 *     </tr>
 *   </tbody>
 * </table>
 * 
 * <p>Por que a derivação depende do contexto completo da unidade?
 * O escopo SOCIAL de um PROPRIETARIO depende da existência de outro RESIDENTE ativo na unidade.
 * Por essa razão, a lógica opera sobre o Aggregate Root {@link Unidade} como um todo, garantindo 
 * a consistência transacional impossível de se manter ao nível de registros individuais de banco.
 * 
 * <p>Anotações aplicadas:
 * <ul>
 *   <li>{@code @Service} — Registra a classe como componente de lógica de negócio do Spring.</li>
 * </ul>
 */
@Service
public class EscopoDerivacaoService {

    /**
     * Conjunto imutável contendo todos os escopos possíveis da aplicação.
     */
    private static final Set<Escopo> TODOS_OS_ESCOPOS =
        EnumSet.of(Escopo.SOCIAL, Escopo.LEGAL, Escopo.FINANCEIRO);

    /**
     * Conjunto imutável contendo exclusivamente o escopo SOCIAL (destinado a inquilinos).
     */
    private static final Set<Escopo> APENAS_SOCIAL =
        EnumSet.of(Escopo.SOCIAL);

    /**
     * Conjunto imutável contendo os escopos de responsabilidade de propriedade (LEGAL e FINANCEIRO).
     */
    private static final Set<Escopo> LEGAL_E_FINANCEIRO =
        EnumSet.of(Escopo.LEGAL, Escopo.FINANCEIRO);

    /**
     * Recalcula e atualiza os escopos de todas as vinculações ATIVAS contidas na unidade residencial informada.
     * 
     * <p>Este método deve ser invocado sempre que uma vinculação for adicionada, removida ou sofrer alterações
     * de status/tipo na unidade.
     * 
     * <p>Nota transacional importante:
     * Após a execução deste método, o chamador (caller) deve obrigatoriamente persistir as alterações da
     * unidade invocando o repositório {@code UnidadeRepository#save(Unidade)}.
     * 
     * @param unidade O Aggregate Root {@link Unidade} cuja coleção de vinculações será recalculada.
     */
    public void derivarEscoposDaUnidade(Unidade unidade) {
        boolean haAlgumResidente = unidade.getVinculacoes().stream()
            .filter(v -> v.getStatus() == StatusVinculacao.ATIVA)
            .anyMatch(this::eResidente);

        for (Vinculacao v : unidade.getVinculacoes()) {
            if (v.getStatus() != StatusVinculacao.ATIVA) {
                continue;
            }
            Set<Escopo> escopos = calcularEscopos(v.getTipo(), haAlgumResidente);
            v.atualizarEscopos(escopos);
        }
    }

    /**
     * Função pura (Side-Effect Free) responsável pelo cálculo matemático/lógico dos escopos.
     * 
     * <p>A divisão deste método em uma função isolada facilita a escrita de testes unitários rápidos,
     * eliminando a necessidade de mockar instâncias complexas do Spring Data ou do banco de dados.
     * 
     * <p>O uso da switch expression do Java garante em tempo de compilação que todas as variantes 
     * de {@link TipoVinculacao} sejam cobertas, evitando bugs em caso de futuras adições no enum.
     * 
     * @param tipo O {@link TipoVinculacao} da vinculação específica analisada.
     * @param unidadeTemResidente Indica se existe outro residente ativo (inquilino/morador) na unidade.
     * @return Um conjunto {@link Set} contendo os privilégios resultantes do cálculo.
     */
    public Set<Escopo> calcularEscopos(TipoVinculacao tipo, boolean unidadeTemResidente) {
        return switch (tipo) {
            case PROPRIETARIO_RESIDENTE ->
                EnumSet.copyOf(TODOS_OS_ESCOPOS);

            case RESIDENTE ->
                EnumSet.copyOf(APENAS_SOCIAL);

            case PROPRIETARIO ->
                unidadeTemResidente
                    ? EnumSet.copyOf(LEGAL_E_FINANCEIRO)
                    : EnumSet.copyOf(TODOS_OS_ESCOPOS);
        };
    }

    /**
     * Verifica se o tipo de vínculo indica residência física na unidade habitacional.
     * 
     * @param v A vinculação a ser inspecionada.
     * @return {@code true} se for morador ou proprietário residente, {@code false} caso contrário.
     */
    private boolean eResidente(Vinculacao v) {
        return v.getTipo() == TipoVinculacao.RESIDENTE
            || v.getTipo() == TipoVinculacao.PROPRIETARIO_RESIDENTE;
    }
}
