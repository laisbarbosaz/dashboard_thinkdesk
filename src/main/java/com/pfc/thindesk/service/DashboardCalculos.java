package com.pfc.thindesk.service;

import com.pfc.thindesk.dto.*;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.StatusChamado;
import com.pfc.thindesk.exception.ParametroInvalidoException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Regras de cálculo dos indicadores do dashboard.
 *
 * Não depende do Spring nem do MongoDB: recebe a lista de chamados já filtrada
 * pelo banco e devolve os números prontos. Isso deixa a regra de negócio fácil
 * de testar de forma isolada (ver DashboardCalculosTest).
 */
public final class DashboardCalculos {

    public static final String NAO_INFORMADO = "Não informado";
    /** Limite de pontos no gráfico, para evitar respostas gigantes (ex.: 5 anos agrupados por dia). */
    public static final int MAX_PONTOS_PERIODO = 400;

    private static final DateTimeFormatter FMT_DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FMT_MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private DashboardCalculos() {
    }

    public static ResumoDTO resumo(List<Chamado> chamados, LocalDateTime agora, int prazoHoras) {
        Map<StatusChamado, Long> porStatus = contarPorStatus(chamados);
        long abertos = porStatus.getOrDefault(StatusChamado.ABERTO, 0L);
        long emAndamento = porStatus.getOrDefault(StatusChamado.EM_ANDAMENTO, 0L);
        long concluidos = porStatus.getOrDefault(StatusChamado.CONCLUIDO, 0L);
        long cancelados = porStatus.getOrDefault(StatusChamado.CANCELADO, 0L);
        long semStatus = chamados.stream().filter(c -> c.statusNormalizado() == null).count();

        long foraDoPrazo = chamados.stream()
                .filter(c -> foraDoPrazo(c, agora, prazoHoras))
                .count();

        long baseResolucao = chamados.size() - cancelados;
        Double taxa = baseResolucao > 0 ? arredondar(100.0 * concluidos / baseResolucao) : null;

        return new ResumoDTO(
                chamados.size(), abertos, emAndamento, concluidos, cancelados,
                abertos + emAndamento, foraDoPrazo, prazoHoras,
                taxa, tempoMedioResolucao(chamados), semStatus, agora);
    }

    /**
     * Série com a quantidade de chamados abertos e concluídos em cada intervalo.
     * Um chamado aberto em março e fechado em abril conta como "aberto" em março
     * e "concluído" em abril.
     */
    public static PeriodoDTO periodo(List<Chamado> chamados, FiltroDashboard filtro,
                                     Agrupamento agrupamento, LocalDateTime agora) {
        LocalDate inicio = filtro.dataInicio();
        LocalDate fim = filtro.dataFim();

        if (inicio == null || fim == null) {
            List<LocalDate> datas = new ArrayList<>();
            for (Chamado c : chamados) {
                if (c.getDataAbertura() != null) datas.add(c.getDataAbertura().toLocalDate());
                if (c.getDataFechamento() != null) datas.add(c.getDataFechamento().toLocalDate());
            }
            if (datas.isEmpty()) {
                return new PeriodoDTO(agrupamento.name().toLowerCase(), 0, 0, List.of(), agora);
            }
            if (inicio == null) inicio = Collections.min(datas);
            if (fim == null) fim = Collections.max(datas);
        }

        LinkedHashMap<LocalDate, long[]> intervalos = new LinkedHashMap<>();
        LocalDate cursor = inicioDoIntervalo(inicio, agrupamento);
        while (!cursor.isAfter(fim)) {
            intervalos.put(cursor, new long[2]);
            if (intervalos.size() > MAX_PONTOS_PERIODO) {
                throw new ParametroInvalidoException("agrupamento",
                        "O período escolhido gera mais de " + MAX_PONTOS_PERIODO
                                + " pontos. Reduza o intervalo de datas ou use agrupamento=semana ou mes.");
            }
            cursor = proximoIntervalo(cursor, agrupamento);
        }

        long totalAbertos = 0;
        long totalConcluidos = 0;
        for (Chamado c : chamados) {
            if (dentro(c.getDataAbertura(), inicio, fim)) {
                long[] v = intervalos.get(inicioDoIntervalo(c.getDataAbertura().toLocalDate(), agrupamento));
                if (v != null) {
                    v[0]++;
                    totalAbertos++;
                }
            }
            if (c.statusNormalizado() == StatusChamado.CONCLUIDO && dentro(c.getDataFechamento(), inicio, fim)) {
                long[] v = intervalos.get(inicioDoIntervalo(c.getDataFechamento().toLocalDate(), agrupamento));
                if (v != null) {
                    v[1]++;
                    totalConcluidos++;
                }
            }
        }

        List<PontoPeriodoDTO> pontos = intervalos.entrySet().stream()
                .map(e -> new PontoPeriodoDTO(e.getKey(), rotulo(e.getKey(), agrupamento),
                        e.getValue()[0], e.getValue()[1]))
                .toList();

        return new PeriodoDTO(agrupamento.name().toLowerCase(), totalAbertos, totalConcluidos, pontos, agora);
    }

    public static DistribuicaoDTO situacao(List<Chamado> chamados, LocalDateTime agora) {
        Map<StatusChamado, Long> porStatus = contarPorStatus(chamados);
        long total = chamados.size();
        List<ContagemDTO> itens = new ArrayList<>();
        for (StatusChamado s : StatusChamado.values()) {
            long qtd = porStatus.getOrDefault(s, 0L);
            itens.add(new ContagemDTO(s.getRotulo(), qtd, percentual(qtd, total)));
        }
        long semStatus = chamados.stream().filter(c -> c.statusNormalizado() == null).count();
        if (semStatus > 0) {
            itens.add(new ContagemDTO(NAO_INFORMADO, semStatus, percentual(semStatus, total)));
        }
        return new DistribuicaoDTO(total, itens, agora);
    }

    public static List<TipoRankingDTO> rankingTipos(List<Chamado> chamados, int limite) {
        Map<String, List<Chamado>> porTipo = agrupar(chamados, Chamado::getTipo);
        long total = chamados.size();

        List<Map.Entry<String, List<Chamado>>> ordenado = porTipo.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, List<Chamado>>>comparingInt(e -> e.getValue().size())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limite)
                .toList();

        List<TipoRankingDTO> ranking = new ArrayList<>();
        int posicao = 1;
        for (Map.Entry<String, List<Chamado>> e : ordenado) {
            long qtd = e.getValue().size();
            long concluidos = e.getValue().stream()
                    .filter(c -> c.statusNormalizado() == StatusChamado.CONCLUIDO).count();
            ranking.add(new TipoRankingDTO(posicao++, e.getKey(), qtd, concluidos, percentual(qtd, total)));
        }
        return ranking;
    }

    public static List<TecnicoDTO> desempenhoTecnicos(List<Chamado> chamados, int limite) {
        return agrupar(chamados, Chamado::getTecnico).entrySet().stream()
                .map(e -> {
                    List<Chamado> lista = e.getValue();
                    long concluidos = lista.stream()
                            .filter(c -> c.statusNormalizado() == StatusChamado.CONCLUIDO).count();
                    long pendentes = lista.stream()
                            .filter(c -> c.statusNormalizado() != null && c.statusNormalizado().isPendente()).count();
                    return new TecnicoDTO(e.getKey(), lista.size(), concluidos, pendentes,
                            tempoMedioResolucao(lista));
                })
                .sorted(Comparator.comparingLong(TecnicoDTO::concluidos).reversed()
                        .thenComparing(Comparator.comparingLong(TecnicoDTO::atribuidos).reversed())
                        .thenComparing(TecnicoDTO::tecnico))
                .limit(limite)
                .toList();
    }

    public static PaginaDTO<ChamadoListaDTO> paginar(List<Chamado> chamados, int pagina, int tamanho,
                                                     LocalDateTime agora, int prazoHoras) {
        List<Chamado> ordenados = new ArrayList<>(chamados);
        ordenados.sort(Comparator.comparing(Chamado::getDataAbertura,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int totalPaginas = (int) Math.ceil(ordenados.size() / (double) tamanho);
        int de = Math.min(pagina * tamanho, ordenados.size());
        int ate = Math.min(de + tamanho, ordenados.size());

        List<ChamadoListaDTO> itens = ordenados.subList(de, ate).stream()
                .map(c -> paraLista(c, agora, prazoHoras))
                .toList();
        return new PaginaDTO<>(itens, pagina, tamanho, ordenados.size(), totalPaginas);
    }

    public static ChamadoListaDTO paraLista(Chamado c, LocalDateTime agora, int prazoHoras) {
        StatusChamado s = c.statusNormalizado();
        Double horas = null;
        boolean semFim = s == StatusChamado.CANCELADO && c.getDataFechamento() == null;
        if (c.getDataAbertura() != null && !semFim) {
            LocalDateTime fim = c.getDataFechamento() != null ? c.getDataFechamento() : agora;
            horas = horas(c.getDataAbertura(), fim);
        }
        return new ChamadoListaDTO(c.getId(), c.getDescricao(),
                s != null ? s.getRotulo() : NAO_INFORMADO,
                textoOuPadrao(c.getTipo()), textoOuPadrao(c.getTecnico()), c.getUsuario(),
                c.getPrioridade(), c.getConsideracoesTecnicas(),
                c.getDataAbertura(), c.getDataFechamento(), horas,
                foraDoPrazo(c, agora, prazoHoras));
    }

    public static OpcoesFiltroDTO opcoes(List<Chamado> chamados) {
        List<String> status = Arrays.stream(StatusChamado.values()).map(StatusChamado::getRotulo).toList();
        return new OpcoesFiltroDTO(status,
                distintos(chamados, Chamado::getTipo),
                distintos(chamados, Chamado::getTecnico),
                distintos(chamados, Chamado::getPrioridade));
    }

    /** Pendente há mais tempo que o prazo configurado. */
    static boolean foraDoPrazo(Chamado c, LocalDateTime agora, int prazoHoras) {
        StatusChamado s = c.statusNormalizado();
        return s != null && s.isPendente() && c.getDataAbertura() != null
                && Duration.between(c.getDataAbertura(), agora).toHours() >= prazoHoras;
    }

    static Double tempoMedioResolucao(List<Chamado> chamados) {
        OptionalDouble media = chamados.stream()
                .filter(c -> c.statusNormalizado() == StatusChamado.CONCLUIDO)
                .filter(c -> c.getDataAbertura() != null && c.getDataFechamento() != null)
                .filter(c -> !c.getDataFechamento().isBefore(c.getDataAbertura()))
                .mapToDouble(c -> Duration.between(c.getDataAbertura(), c.getDataFechamento()).toMinutes() / 60.0)
                .average();
        return media.isPresent() ? arredondar(media.getAsDouble()) : null;
    }

    private static Map<StatusChamado, Long> contarPorStatus(List<Chamado> chamados) {
        return chamados.stream()
                .map(Chamado::statusNormalizado)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(s -> s, () -> new EnumMap<>(StatusChamado.class), Collectors.counting()));
    }

    private static Map<String, List<Chamado>> agrupar(List<Chamado> chamados, Function<Chamado, String> campo) {
        return chamados.stream().collect(Collectors.groupingBy(c -> textoOuPadrao(campo.apply(c))));
    }

    private static List<String> distintos(List<Chamado> chamados, Function<Chamado, String> campo) {
        return chamados.stream()
                .map(campo)
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static String textoOuPadrao(String valor) {
        return valor == null || valor.isBlank() ? NAO_INFORMADO : valor.trim();
    }

    private static boolean dentro(LocalDateTime data, LocalDate inicio, LocalDate fim) {
        if (data == null) return false;
        LocalDate d = data.toLocalDate();
        return !d.isBefore(inicio) && !d.isAfter(fim);
    }

    private static LocalDate inicioDoIntervalo(LocalDate data, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> data;
            case SEMANA -> data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> data.withDayOfMonth(1);
        };
    }

    private static LocalDate proximoIntervalo(LocalDate data, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> data.plusDays(1);
            case SEMANA -> data.plusWeeks(1);
            case MES -> data.plusMonths(1);
        };
    }

    private static String rotulo(LocalDate data, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> data.format(FMT_DIA);
            case SEMANA -> "Sem. " + data.format(FMT_DIA);
            case MES -> data.format(FMT_MES);
        };
    }

    private static double percentual(long parte, long total) {
        return total == 0 ? 0.0 : arredondar(100.0 * parte / total);
    }

    private static double horas(LocalDateTime de, LocalDateTime ate) {
        return arredondar(Duration.between(de, ate).toMinutes() / 60.0);
    }

    private static double arredondar(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
