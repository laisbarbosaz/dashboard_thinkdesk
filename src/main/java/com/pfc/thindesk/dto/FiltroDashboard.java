package com.pfc.thindesk.dto;

import com.pfc.thindesk.entity.StatusChamado;
import com.pfc.thindesk.exception.ParametroInvalidoException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Filtros aceitos por todos os endpoints do dashboard.
 * Campos nulos significam "sem filtro".
 */
public record FiltroDashboard(
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusChamado status,
        String tipo,
        String tecnico,
        String prioridade) {

    public static final FiltroDashboard VAZIO = new FiltroDashboard(null, null, null, null, null, null);

    /** Monta o filtro a partir dos parâmetros da URL, validando cada um. */
    public static FiltroDashboard de(String dataInicio, String dataFim, String status,
                                     String tipo, String tecnico, String prioridade) {
        LocalDate inicio = lerData("dataInicio", dataInicio);
        LocalDate fim = lerData("dataFim", dataFim);

        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new ParametroInvalidoException("dataInicio",
                    "dataInicio (" + inicio + ") não pode ser posterior a dataFim (" + fim + ").");
        }

        StatusChamado statusFiltro = null;
        if (temValor(status)) {
            statusFiltro = StatusChamado.normalizar(status);
            if (statusFiltro == null) {
                throw new ParametroInvalidoException("status",
                        "Status '" + status + "' não reconhecido. Use: ABERTO, EM_ANDAMENTO, CONCLUIDO ou CANCELADO.");
            }
        }

        return new FiltroDashboard(inicio, fim, statusFiltro,
                limpar(tipo), limpar(tecnico), limpar(prioridade));
    }

    /** Início do período (inclusivo), ou null. */
    public LocalDateTime inicioPeriodo() {
        return dataInicio == null ? null : dataInicio.atStartOfDay();
    }

    /** Fim do período (exclusivo: meia-noite do dia seguinte), ou null. */
    public LocalDateTime fimPeriodo() {
        return dataFim == null ? null : dataFim.plusDays(1).atStartOfDay();
    }

    public boolean temPeriodo() {
        return dataInicio != null || dataFim != null;
    }

    private static LocalDate lerData(String nome, String valor) {
        if (!temValor(valor)) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new ParametroInvalidoException(nome,
                    "Data inválida em '" + nome + "': '" + valor + "'. Use o formato AAAA-MM-DD.");
        }
    }

    private static boolean temValor(String valor) {
        return valor != null && !valor.isBlank() && !valor.equalsIgnoreCase("todos");
    }

    private static String limpar(String valor) {
        return temValor(valor) ? valor.trim() : null;
    }
}
