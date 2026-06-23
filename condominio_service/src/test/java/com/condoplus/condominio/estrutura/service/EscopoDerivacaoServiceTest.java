package com.condoplus.condominio.estrutura.service;

import com.condoplus.condominio.estrutura.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EscopoDerivacaoServiceTest {

    private final EscopoDerivacaoService service = new EscopoDerivacaoService();

    @Test
    void proprietarioResidenteSemprePossuiTodosOsEscopos() {

        Set<Escopo> semResidente = service.calcularEscopos(TipoVinculacao.PROPRIETARIO_RESIDENTE, false);
        assertThat(semResidente).containsExactlyInAnyOrder(Escopo.SOCIAL, Escopo.LEGAL, Escopo.FINANCEIRO);

        Set<Escopo> comResidente = service.calcularEscopos(TipoVinculacao.PROPRIETARIO_RESIDENTE, true);
        assertThat(comResidente).containsExactlyInAnyOrder(Escopo.SOCIAL, Escopo.LEGAL, Escopo.FINANCEIRO);
    }

    @Test
    void residenteSomenteSocialIndependenteDoContexto() {

        Set<Escopo> result = service.calcularEscopos(TipoVinculacao.RESIDENTE, true);
        assertThat(result).containsExactly(Escopo.SOCIAL);

        Set<Escopo> result2 = service.calcularEscopos(TipoVinculacao.RESIDENTE, false);
        assertThat(result2).containsExactly(Escopo.SOCIAL);
    }

    @Test
    void proprietarioComInquilinoTemApenasLegalEFinanceiro() {

        Set<Escopo> result = service.calcularEscopos(TipoVinculacao.PROPRIETARIO, true);
        assertThat(result).containsExactlyInAnyOrder(Escopo.LEGAL, Escopo.FINANCEIRO);
        assertThat(result).doesNotContain(Escopo.SOCIAL);
    }

    @Test
    void proprietarioComAptoVazioTemTodosOsEscopos() {

        Set<Escopo> result = service.calcularEscopos(TipoVinculacao.PROPRIETARIO, false);
        assertThat(result).containsExactlyInAnyOrder(Escopo.SOCIAL, Escopo.LEGAL, Escopo.FINANCEIRO);
    }

    @Test
    void derivarSobreUnidadeCompletaAplicaRegrasParaTodos() {

        Unidade u = Unidade.criar("101", "A", TipoUnidade.APARTAMENTO);
        Vinculacao prop = Vinculacao.criar(UUID.randomUUID(), TipoVinculacao.PROPRIETARIO, LocalDate.now());
        Vinculacao resi = Vinculacao.criar(UUID.randomUUID(), TipoVinculacao.RESIDENTE, LocalDate.now());
        u.adicionarVinculacao(prop);
        u.adicionarVinculacao(resi);

        service.derivarEscoposDaUnidade(u);

        assertThat(prop.getEscoposComoEnum())
            .containsExactlyInAnyOrder(Escopo.LEGAL, Escopo.FINANCEIRO);
        assertThat(resi.getEscoposComoEnum())
            .containsExactly(Escopo.SOCIAL);
    }

    @Test
    void vinculacoesEncerradasNaoInfluenciamDerivacao() {

        Unidade u = Unidade.criar("202", "B", TipoUnidade.APARTAMENTO);
        Vinculacao prop = Vinculacao.criar(UUID.randomUUID(), TipoVinculacao.PROPRIETARIO, LocalDate.now().minusYears(2));
        Vinculacao resiEncerrado = Vinculacao.criar(UUID.randomUUID(), TipoVinculacao.RESIDENTE, LocalDate.now().minusYears(1));
        resiEncerrado.encerrar(LocalDate.now().minusDays(30));

        u.adicionarVinculacao(prop);
        u.adicionarVinculacao(resiEncerrado);

        service.derivarEscoposDaUnidade(u);

        assertThat(prop.getEscoposComoEnum())
            .containsExactlyInAnyOrder(Escopo.SOCIAL, Escopo.LEGAL, Escopo.FINANCEIRO);
    }
}
