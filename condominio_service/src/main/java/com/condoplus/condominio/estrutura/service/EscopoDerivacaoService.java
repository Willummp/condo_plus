package com.condoplus.condominio.estrutura.service;

import com.condoplus.condominio.estrutura.domain.*;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class EscopoDerivacaoService {

    
    private static final Set<Escopo> TODOS_OS_ESCOPOS =
        EnumSet.of(Escopo.SOCIAL, Escopo.LEGAL, Escopo.FINANCEIRO);

    
    private static final Set<Escopo> APENAS_SOCIAL =
        EnumSet.of(Escopo.SOCIAL);

    
    private static final Set<Escopo> LEGAL_E_FINANCEIRO =
        EnumSet.of(Escopo.LEGAL, Escopo.FINANCEIRO);

    
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

    
    private boolean eResidente(Vinculacao v) {
        return v.getTipo() == TipoVinculacao.RESIDENTE
            || v.getTipo() == TipoVinculacao.PROPRIETARIO_RESIDENTE;
    }
}
