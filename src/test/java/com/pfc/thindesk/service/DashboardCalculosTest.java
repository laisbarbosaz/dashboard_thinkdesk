package com.pfc.thindesk.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pfc.thindesk.dto.*;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.exception.ParametroInvalidoException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardCalculosTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 14, 12, 0);

    private static Chamado chamado(String status, String tipo, String tecnico,
                                   LocalDateTime abertura, LocalDateTime fechamento) {
        Chamado c = new Chamado();
        c.setStatus(status);
        c.setTipo(tipo);
        c.setTecnico(tecnico);
        c.setDataAbertura(abertura);
        c.setDataFechamento(fechamento);
        return c;
    }

    private static List<Chamado> base() {
        return List.of(
                chamado("Concluído", "Rede", "João", AGORA.minusDays(5), AGORA.minusDays(5).plusHours(4)),
                chamado("Concluído", "Rede", "Mariana", AGORA.minusDays(4), AGORA.minusDays(4).plusHours(8)),
                chamado("CONCLUIDO", "Impressora", "João", AGORA.minusDays(3), AGORA.minusDays(3).plusHours(6)),
                chamado("Aberto", "Rede", "João", AGORA.minusDays(3), null),
                chamado("EM_ANDAMENTO", "Hardware", "Mariana", AGORA.minusHours(10), null),
                chamado("Cancelado", "Impressora", null, AGORA.minusDays(1), null),
                chamado("qualquer coisa", "", "João", null, null));
    }

    @Test
    void resumoContaCadaSituacaoENormalizaStatus() {
        ResumoDTO r = DashboardCalculos.resumo(base(), AGORA, 48);

        assertEquals(7, r.total());
        assertEquals(1, r.abertos());
        assertEquals(1, r.emAndamento());
        assertEquals(3, r.concluidos());
        assertEquals(1, r.cancelados());
        assertEquals(2, r.pendentes());
        assertEquals(1, r.semStatusDefinido());
        assertEquals(1, r.pendentesForaDoPrazo());
        assertEquals(50.0, r.taxaResolucao());
        assertEquals(6.0, r.tempoMedioResolucaoHoras());
    }

    @Test
    void resumoDeListaVaziaNaoQuebra() {
        ResumoDTO r = DashboardCalculos.resumo(List.of(), AGORA, 48);
        assertEquals(0, r.total());
        assertNull(r.taxaResolucao());
        assertNull(r.tempoMedioResolucaoHoras());
    }

    @Test
    void tempoMedioIgnoraFechamentoAntesDaAbertura() {
        List<Chamado> lista = List.of(
                chamado("Concluído", "Rede", "João", AGORA.minusHours(10), AGORA.minusHours(8)),
                chamado("Concluído", "Rede", "João", AGORA.minusHours(5), AGORA.minusHours(9)));
        assertEquals(2.0, DashboardCalculos.tempoMedioResolucao(lista));
    }

    @Test
    void periodoPorDiaPreencheDiasVaziosEContaFechamentoNoDiaCerto() {
        LocalDate fim = AGORA.toLocalDate();
        FiltroDashboard filtro = new FiltroDashboard(fim.minusDays(6), fim, null, null, null, null);

        PeriodoDTO p = DashboardCalculos.periodo(base(), filtro, Agrupamento.DIA, AGORA);

        assertEquals(7, p.pontos().size());
        assertEquals(fim.minusDays(6), p.pontos().get(0).inicio());
        assertEquals(6, p.totalAbertos());
        assertEquals(3, p.totalConcluidos());
        PontoPeriodoDTO dia3 = p.pontos().stream()
                .filter(x -> x.inicio().equals(fim.minusDays(3))).findFirst().orElseThrow();
        assertEquals(2, dia3.abertos());
        assertEquals(1, dia3.concluidos());
    }

    @Test
    void periodoPorMesAgrupaNoPrimeiroDia() {
        FiltroDashboard filtro = new FiltroDashboard(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 11, 14),
                null, null, null, null);
        PeriodoDTO p = DashboardCalculos.periodo(base(), filtro, Agrupamento.MES, AGORA);

        assertEquals(3, p.pontos().size());
        assertEquals(LocalDate.of(2026, 9, 1), p.pontos().get(0).inicio());
        assertEquals("11/2026", p.pontos().get(2).rotulo());
        assertEquals(6, p.pontos().get(2).abertos());
    }

    @Test
    void periodoPorSemanaComecaNaSegunda() {
        FiltroDashboard filtro = new FiltroDashboard(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 14),
                null, null, null, null);
        PeriodoDTO p = DashboardCalculos.periodo(List.of(), filtro, Agrupamento.SEMANA, AGORA);
        assertEquals(1, p.pontos().size());
        assertEquals(LocalDate.of(2026, 11, 9), p.pontos().get(0).inicio());
    }

    @Test
    void periodoMuitoLongoPorDiaRetornaErro() {
        FiltroDashboard filtro = new FiltroDashboard(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1),
                null, null, null, null);
        assertThrows(ParametroInvalidoException.class,
                () -> DashboardCalculos.periodo(List.of(), filtro, Agrupamento.DIA, AGORA));
    }

    @Test
    void periodoSemDatasUsaIntervaloDosDados() {
        PeriodoDTO p = DashboardCalculos.periodo(base(), FiltroDashboard.VAZIO, Agrupamento.DIA, AGORA);
        assertEquals(AGORA.toLocalDate().minusDays(5), p.pontos().get(0).inicio());
        assertEquals(AGORA.toLocalDate(), p.pontos().get(p.pontos().size() - 1).inicio());
    }

    @Test
    void situacaoTrazTodosOsStatusNaOrdemEOsNaoInformados() {
        DistribuicaoDTO d = DashboardCalculos.situacao(base(), AGORA);
        assertEquals(5, d.itens().size());
        assertEquals("Aberto", d.itens().get(0).nome());
        assertEquals("Concluído", d.itens().get(2).nome());
        assertEquals(3, d.itens().get(2).quantidade());
        assertEquals(42.9, d.itens().get(2).percentual());
        assertEquals(DashboardCalculos.NAO_INFORMADO, d.itens().get(4).nome());
    }

    @Test
    void rankingDeTiposOrdenaPorQuantidadeERespeitaLimite() {
        List<TipoRankingDTO> ranking = DashboardCalculos.rankingTipos(base(), 2);
        assertEquals(2, ranking.size());
        assertEquals("Rede", ranking.get(0).tipo());
        assertEquals(3, ranking.get(0).total());
        assertEquals(2, ranking.get(0).concluidos());
        assertEquals(1, ranking.get(0).posicao());
        assertEquals("Impressora", ranking.get(1).tipo());
    }

    @Test
    void tecnicosOrdenadosPorConcluidos() {
        List<TecnicoDTO> t = DashboardCalculos.desempenhoTecnicos(base(), 10);
        assertEquals("João", t.get(0).tecnico());
        assertEquals(4, t.get(0).atribuidos());
        assertEquals(2, t.get(0).concluidos());
        assertEquals(1, t.get(0).pendentes());
        assertTrue(t.stream().anyMatch(x -> x.tecnico().equals(DashboardCalculos.NAO_INFORMADO)));
    }

    @Test
    void paginacaoOrdenaDoMaisRecenteParaOMaisAntigo() {
        PaginaDTO<ChamadoListaDTO> p1 = DashboardCalculos.paginar(base(), 0, 3, AGORA, 48);
        assertEquals(7, p1.totalItens());
        assertEquals(3, p1.totalPaginas());
        assertEquals(3, p1.itens().size());
        assertEquals("Em andamento", p1.itens().get(0).status());

        PaginaDTO<ChamadoListaDTO> p3 = DashboardCalculos.paginar(base(), 2, 3, AGORA, 48);
        assertEquals(1, p3.itens().size());
        assertNull(p3.itens().get(0).dataAbertura());
        assertEquals(DashboardCalculos.NAO_INFORMADO, p3.itens().get(0).status());

        PaginaDTO<ChamadoListaDTO> alem = DashboardCalculos.paginar(base(), 9, 3, AGORA, 48);
        assertTrue(alem.itens().isEmpty());
    }

    @Test
    void opcoesDeFiltroSemValoresVaziosERepetidos() {
        OpcoesFiltroDTO o = DashboardCalculos.opcoes(base());
        assertEquals(List.of("Hardware", "Impressora", "Rede"), o.tipos());
        assertEquals(List.of("João", "Mariana"), o.tecnicos());
        assertEquals(4, o.status().size());
    }
}
