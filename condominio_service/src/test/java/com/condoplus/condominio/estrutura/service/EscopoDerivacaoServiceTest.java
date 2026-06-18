package com.condoplus.condominio.estrutura.service;

import com.condoplus.condominio.estrutura.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários da derivação de escopos.
 *
 * <p>Por que este é o teste mais importante do serviço?
 * EscopoDerivacaoService implementa a invariante central do bounded context:
 * quem pode fazer o quê em uma unidade. Se a derivação estiver errada,
 * um inquilino pode tomar decisões de proprietário, ou um proprietário
 * perde acesso ao cotidiano da unidade.
 *
 * <p>Por que não precisa de Spring context, banco ou mocks?
 * calcularEscopos() é uma função pura — mesmos inputs, mesmos outputs,
 * sem efeitos colaterais. É o tipo de lógica mais fácil e mais valioso de testar.
 */
class EscopoDerivacaoServiceTest {

    // Instancia diretamente — sem Spring, sem @ExtendWith(MockitoExtension.class)
    private final EscopoDerivacaoService service = new EscopoDerivacaoService();

    @Test
    void proprietarioResidenteSemprePossuiTodosOsEscopos() {
        // PROPRIETARIO_RESIDENTE: dono E mora — tem tudo, independente do contexto
        Set<Escopo> semResidente = service.calcularEscopos(TipoVinculacao.PROPRIETARIO_RESIDENTE, false);
        assertThat(semResidente).containsExactlyInAnyOrder(Escopo.SOCIAL, Escopo.LEGAL, Escopo.FINANCEIRO);

        Set<Escopo> comResidente = service.calcularEscopos(TipoVinculacao.PROPRIETARIO_RESIDENTE, true);
        assertThat(comResidente).containsExactlyInAnyOrder(Escopo.SOCIAL, Escopo.LEGAL, Escopo.FINANCEIRO);
    }

    @Test
    void residenteSomenteSocialIndependenteDoContexto() {
        // RESIDENTE (inquilino): apenas cotidiano — Lei do Inquilinato 8.245/91
        Set<Escopo> result = service.calcularEscopos(TipoVinculacao.RESIDENTE, true);
        assertThat(result).containsExactly(Escopo.SOCIAL);

        Set<Escopo> result2 = service.calcularEscopos(TipoVinculacao.RESIDENTE, false);
        assertThat(result2).containsExactly(Escopo.SOCIAL);
    }

    @Test
    void proprietarioComInquilinoTemApenasLegalEFinanceiro() {
        // Apto alugado: proprietário cede o SOCIAL ao inquilino
        Set<Escopo> result = service.calcularEscopos(TipoVinculacao.PROPRIETARIO, true);
        assertThat(result).containsExactlyInAnyOrder(Escopo.LEGAL, Escopo.FINANCEIRO);
        assertThat(result).doesNotContain(Escopo.SOCIAL);
    }

    @Test
    void proprietarioComAptoVazioTemTodosOsEscopos() {
        // Apto vazio: proprietário acumula SOCIAL também (quem vai autorizar prestadores?)
        Set<Escopo> result = service.calcularEscopos(TipoVinculacao.PROPRIETARIO, false);
        assertThat(result).containsExactlyInAnyOrder(Escopo.SOCIAL, Escopo.LEGAL, Escopo.FINANCEIRO);
    }

    @Test
    void derivarSobreUnidadeCompletaAplicaRegrasParaTodos() {
        // Cenário: unidade 101-A com PROPRIETARIO + RESIDENTE
        // Proprietário deve perder SOCIAL (tem inquilino)
        // Residente deve ter apenas SOCIAL

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
        // Se o residente saiu (status ENCERRADA), o proprietário volta a ter SOCIAL
        Unidade u = Unidade.criar("202", "B", TipoUnidade.APARTAMENTO);
        Vinculacao prop = Vinculacao.criar(UUID.randomUUID(), TipoVinculacao.PROPRIETARIO, LocalDate.now().minusYears(2));
        Vinculacao resiEncerrado = Vinculacao.criar(UUID.randomUUID(), TipoVinculacao.RESIDENTE, LocalDate.now().minusYears(1));
        resiEncerrado.encerrar(LocalDate.now().minusDays(30)); // ENCERRADA

        u.adicionarVinculacao(prop);
        u.adicionarVinculacao(resiEncerrado);

        service.derivarEscoposDaUnidade(u);

        // Sem residente ATIVO, proprietário recupera SOCIAL
        assertThat(prop.getEscoposComoEnum())
            .containsExactlyInAnyOrder(Escopo.SOCIAL, Escopo.LEGAL, Escopo.FINANCEIRO);
    }
}
