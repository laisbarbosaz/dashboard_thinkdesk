package com.pfc.thindesk.service;

import com.pfc.thindesk.dto.*;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.exception.ParametroInvalidoException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Busca os chamados no MongoDB aplicando os filtros e entrega os dados
 * para {@link DashboardCalculos} montar os indicadores.
 */
@Service
public class DashboardService {

    private final MongoTemplate mongoTemplate;
    private final int prazoHoras;
    private final Clock relogio;

    @Autowired
    public DashboardService(MongoTemplate mongoTemplate,
                            @Value("${thindesk.dashboard.prazo-horas:48}") int prazoHoras) {
        this(mongoTemplate, prazoHoras, Clock.systemDefaultZone());
    }

    DashboardService(MongoTemplate mongoTemplate, int prazoHoras, Clock relogio) {
        this.mongoTemplate = mongoTemplate;
        this.prazoHoras = prazoHoras;
        this.relogio = relogio;
    }

    public ResumoDTO resumo(FiltroDashboard filtro) {
        return DashboardCalculos.resumo(buscar(filtro, false), agora(), prazoHoras);
    }

    public PeriodoDTO periodo(FiltroDashboard filtro, Agrupamento agrupamento) {
        return DashboardCalculos.periodo(buscar(filtro, true), filtro, agrupamento, agora());
    }

    public DistribuicaoDTO situacao(FiltroDashboard filtro) {
        return DashboardCalculos.situacao(buscar(filtro, false), agora());
    }

    public List<TipoRankingDTO> rankingTipos(FiltroDashboard filtro, int limite) {
        validarFaixa("limite", limite, 1, 50);
        return DashboardCalculos.rankingTipos(buscar(filtro, false), limite);
    }

    public List<TecnicoDTO> tecnicos(FiltroDashboard filtro, int limite) {
        validarFaixa("limite", limite, 1, 50);
        return DashboardCalculos.desempenhoTecnicos(buscar(filtro, false), limite);
    }

    public PaginaDTO<ChamadoListaDTO> chamados(FiltroDashboard filtro, int pagina, int tamanho) {
        validarFaixa("pagina", pagina, 0, Integer.MAX_VALUE);
        validarFaixa("tamanho", tamanho, 1, 100);
        return DashboardCalculos.paginar(buscar(filtro, false), pagina, tamanho, agora(), prazoHoras);
    }

    public OpcoesFiltroDTO opcoesFiltro() {
        return DashboardCalculos.opcoes(mongoTemplate.findAll(Chamado.class));
    }

    /**
     * Monta a consulta no MongoDB.
     *
     * @param incluirFechadosNoPeriodo para o gráfico por período, também traz os chamados
     *        abertos antes do período mas concluídos dentro dele.
     */
    List<Chamado> buscar(FiltroDashboard filtro, boolean incluirFechadosNoPeriodo) {
        List<Criteria> condicoes = new ArrayList<>();

        if (filtro.status() != null) {
            condicoes.add(Criteria.where("status").is(filtro.status().getRotulo()));
        }
        if (filtro.tipo() != null) {
            condicoes.add(Criteria.where("tipo").regex(igualSemCaixa(filtro.tipo())));
        }
        if (filtro.tecnico() != null) {
            condicoes.add(Criteria.where("tecnico").regex(igualSemCaixa(filtro.tecnico())));
        }
        if (filtro.prioridade() != null) {
            condicoes.add(Criteria.where("prioridade").regex(igualSemCaixa(filtro.prioridade())));
        }
        if (filtro.temPeriodo()) {
            Criteria abertura = intervalo("dataAbertura", filtro);
            condicoes.add(incluirFechadosNoPeriodo
                    ? new Criteria().orOperator(abertura, intervalo("dataFechamento", filtro))
                    : abertura);
        }

        Query query = new Query();
        if (!condicoes.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(condicoes));
        }
        return mongoTemplate.find(query, Chamado.class);
    }

    private static Criteria intervalo(String campo, FiltroDashboard filtro) {
        Criteria c = Criteria.where(campo);
        if (filtro.inicioPeriodo() != null) c = c.gte(filtro.inicioPeriodo());
        if (filtro.fimPeriodo() != null) c = c.lt(filtro.fimPeriodo());
        return c;
    }

    /** Comparação exata ignorando maiúsculas/minúsculas e espaços nas pontas. */
    private static Pattern igualSemCaixa(String valor) {
        return Pattern.compile("^\\s*" + Pattern.quote(valor) + "\\s*$", Pattern.CASE_INSENSITIVE);
    }

    private static void validarFaixa(String nome, int valor, int min, int max) {
        if (valor < min || valor > max) {
            String faixa = max == Integer.MAX_VALUE ? "maior ou igual a " + min : "entre " + min + " e " + max;
            throw new ParametroInvalidoException(nome, "O parâmetro '" + nome + "' deve ser " + faixa + ".");
        }
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }
}
