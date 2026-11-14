package com.pfc.thindesk.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pfc.SecurityConfig;
import com.pfc.thindesk.dto.*;
import com.pfc.thindesk.exception.ParametroInvalidoException;
import com.pfc.thindesk.service.DashboardService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardApiController.class)
@Import(SecurityConfig.class)
class DashboardApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DashboardService dashboardService;

    // MongoInitConfig e TratamentoDadosLegados dependem do MongoTemplate
    @MockitoBean
    private MongoTemplate mongoTemplate;

    @Test
    void semLoginRetorna401() throws Exception {
        mvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void resumoRetornaIndicadores() throws Exception {
        when(dashboardService.resumo(any())).thenReturn(new ResumoDTO(10, 2, 3, 4, 1, 5, 1, 48,
                44.4, 6.5, 0, LocalDateTime.of(2026, 11, 14, 12, 0)));

        mvc.perform(get("/api/dashboard/resumo").param("dataInicio", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.concluidos").value(4))
                .andExpect(jsonPath("$.taxaResolucao").value(44.4));
    }

    @Test
    @WithMockUser
    void dataInvalidaRetorna400ComMensagem() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").param("dataInicio", "14-11-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.parametro").value("dataInicio"))
                .andExpect(jsonPath("$.caminho").value("/api/dashboard/resumo"));
    }

    @Test
    @WithMockUser
    void agrupamentoInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/dashboard/periodo").param("agrupamento", "ano"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.parametro").value("agrupamento"));
    }

    @Test
    @WithMockUser
    void limiteNaoNumericoRetorna400() throws Exception {
        mvc.perform(get("/api/dashboard/tipos").param("limite", "dez"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.parametro").value("limite"));
    }

    @Test
    @WithMockUser
    void limiteForaDaFaixaRetorna400() throws Exception {
        when(dashboardService.rankingTipos(any(), eq(500)))
                .thenThrow(new ParametroInvalidoException("limite", "O parâmetro 'limite' deve ser entre 1 e 50."));
        mvc.perform(get("/api/dashboard/tipos").param("limite", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void tiposRetornaRanking() throws Exception {
        when(dashboardService.rankingTipos(any(), eq(5))).thenReturn(List.of(
                new TipoRankingDTO(1, "Rede", 12, 10, 40.0),
                new TipoRankingDTO(2, "Impressora", 8, 8, 26.7)));

        mvc.perform(get("/api/dashboard/tipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("Rede"))
                .andExpect(jsonPath("$[1].posicao").value(2));
    }

    @Test
    @WithMockUser
    void bancoForaDoArRetorna503() throws Exception {
        when(dashboardService.situacao(any())).thenThrow(new DataAccessResourceFailureException("timeout"));
        mvc.perform(get("/api/dashboard/status"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }
}
