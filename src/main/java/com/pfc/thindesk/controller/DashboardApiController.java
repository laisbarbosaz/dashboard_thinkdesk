package com.pfc.thindesk.controller;

import com.pfc.thindesk.dto.*;
import com.pfc.thindesk.service.DashboardService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints que alimentam o Dashboard Administrativo.
 *
 * Todos aceitam os mesmos filtros opcionais:
 * dataInicio, dataFim (AAAA-MM-DD), status, tipo, tecnico e prioridade.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final DashboardService dashboardService;

    public DashboardApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** RF01 — indicadores gerais. */
    @GetMapping("/resumo")
    public ResumoDTO resumo(@RequestParam(required = false) String dataInicio,
                            @RequestParam(required = false) String dataFim,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String tipo,
                            @RequestParam(required = false) String tecnico,
                            @RequestParam(required = false) String prioridade) {
        return dashboardService.resumo(FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade));
    }

    /** RF02 — chamados abertos e concluídos por dia, semana ou mês. */
    @GetMapping("/periodo")
    public PeriodoDTO periodo(@RequestParam(required = false) String dataInicio,
                              @RequestParam(required = false) String dataFim,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String tipo,
                              @RequestParam(required = false) String tecnico,
                              @RequestParam(required = false) String prioridade,
                              @RequestParam(defaultValue = "dia") String agrupamento) {
        return dashboardService.periodo(
                FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade),
                Agrupamento.de(agrupamento));
    }

    /** RF03 — quantidade de chamados em cada situação. */
    @GetMapping("/status")
    public DistribuicaoDTO status(@RequestParam(required = false) String dataInicio,
                                  @RequestParam(required = false) String dataFim,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String tipo,
                                  @RequestParam(required = false) String tecnico,
                                  @RequestParam(required = false) String prioridade) {
        return dashboardService.situacao(FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade));
    }

    /** RF04 — ranking dos tipos de chamado mais frequentes. */
    @GetMapping("/tipos")
    public List<TipoRankingDTO> tipos(@RequestParam(required = false) String dataInicio,
                                      @RequestParam(required = false) String dataFim,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String tipo,
                                      @RequestParam(required = false) String tecnico,
                                      @RequestParam(required = false) String prioridade,
                                      @RequestParam(defaultValue = "5") int limite) {
        return dashboardService.rankingTipos(
                FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade), limite);
    }

    /** Desempenho por técnico (apoio às considerações técnicas). */
    @GetMapping("/tecnicos")
    public List<TecnicoDTO> tecnicos(@RequestParam(required = false) String dataInicio,
                                     @RequestParam(required = false) String dataFim,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String tipo,
                                     @RequestParam(required = false) String tecnico,
                                     @RequestParam(required = false) String prioridade,
                                     @RequestParam(defaultValue = "10") int limite) {
        return dashboardService.tecnicos(
                FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade), limite);
    }

    /** Lista paginada dos chamados que atendem aos filtros. */
    @GetMapping("/chamados")
    public PaginaDTO<ChamadoListaDTO> chamados(@RequestParam(required = false) String dataInicio,
                                               @RequestParam(required = false) String dataFim,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String tipo,
                                               @RequestParam(required = false) String tecnico,
                                               @RequestParam(required = false) String prioridade,
                                               @RequestParam(defaultValue = "0") int pagina,
                                               @RequestParam(defaultValue = "10") int tamanho) {
        return dashboardService.chamados(
                FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade), pagina, tamanho);
    }

    /** RF05 — valores disponíveis para os campos de filtro. */
    @GetMapping("/filtros")
    public OpcoesFiltroDTO filtros() {
        return dashboardService.opcoesFiltro();
    }
}
