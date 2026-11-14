package com.pfc.thindesk.controller;

import com.pfc.thindesk.dto.*;
import com.pfc.thindesk.service.DashboardService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final DashboardService dashboardService;

    public DashboardApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public ResumoDTO resumo(@RequestParam(name = "dataInicio", required = false) String dataInicio,
                            @RequestParam(name = "dataFim", required = false) String dataFim,
                            @RequestParam(name = "status", required = false) String status,
                            @RequestParam(name = "tipo", required = false) String tipo,
                            @RequestParam(name = "tecnico", required = false) String tecnico,
                            @RequestParam(name = "prioridade", required = false) String prioridade) {
        return dashboardService.resumo(FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade));
    }

    @GetMapping("/periodo")
    public PeriodoDTO periodo(@RequestParam(name = "dataInicio", required = false) String dataInicio,
                              @RequestParam(name = "dataFim", required = false) String dataFim,
                              @RequestParam(name = "status", required = false) String status,
                              @RequestParam(name = "tipo", required = false) String tipo,
                              @RequestParam(name = "tecnico", required = false) String tecnico,
                              @RequestParam(name = "prioridade", required = false) String prioridade,
                              @RequestParam(name = "agrupamento", defaultValue = "dia") String agrupamento) {
        return dashboardService.periodo(
                FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade),
                Agrupamento.de(agrupamento));
    }

    @GetMapping("/status")
    public DistribuicaoDTO status(@RequestParam(name = "dataInicio", required = false) String dataInicio,
                                  @RequestParam(name = "dataFim", required = false) String dataFim,
                                  @RequestParam(name = "status", required = false) String status,
                                  @RequestParam(name = "tipo", required = false) String tipo,
                                  @RequestParam(name = "tecnico", required = false) String tecnico,
                                  @RequestParam(name = "prioridade", required = false) String prioridade) {
        return dashboardService.situacao(FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade));
    }

    @GetMapping("/tipos")
    public List<TipoRankingDTO> tipos(@RequestParam(name = "dataInicio", required = false) String dataInicio,
                                      @RequestParam(name = "dataFim", required = false) String dataFim,
                                      @RequestParam(name = "status", required = false) String status,
                                      @RequestParam(name = "tipo", required = false) String tipo,
                                      @RequestParam(name = "tecnico", required = false) String tecnico,
                                      @RequestParam(name = "prioridade", required = false) String prioridade,
                                      @RequestParam(name = "limite", defaultValue = "5") int limite) {
        return dashboardService.rankingTipos(
                FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade), limite);
    }

    @GetMapping("/tecnicos")
    public List<TecnicoDTO> tecnicos(@RequestParam(name = "dataInicio", required = false) String dataInicio,
                                     @RequestParam(name = "dataFim", required = false) String dataFim,
                                     @RequestParam(name = "status", required = false) String status,
                                     @RequestParam(name = "tipo", required = false) String tipo,
                                     @RequestParam(name = "tecnico", required = false) String tecnico,
                                     @RequestParam(name = "prioridade", required = false) String prioridade,
                                     @RequestParam(name = "limite", defaultValue = "10") int limite) {
        return dashboardService.tecnicos(
                FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade), limite);
    }

    @GetMapping("/chamados")
    public PaginaDTO<ChamadoListaDTO> chamados(@RequestParam(name = "dataInicio", required = false) String dataInicio,
                                               @RequestParam(name = "dataFim", required = false) String dataFim,
                                               @RequestParam(name = "status", required = false) String status,
                                               @RequestParam(name = "tipo", required = false) String tipo,
                                               @RequestParam(name = "tecnico", required = false) String tecnico,
                                               @RequestParam(name = "prioridade", required = false) String prioridade,
                                               @RequestParam(name = "pagina", defaultValue = "0") int pagina,
                                               @RequestParam(name = "tamanho", defaultValue = "10") int tamanho) {
        return dashboardService.chamados(
                FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade), pagina, tamanho);
    }

    @GetMapping("/filtros")
    public OpcoesFiltroDTO filtros() {
        return dashboardService.opcoesFiltro();
    }
}
