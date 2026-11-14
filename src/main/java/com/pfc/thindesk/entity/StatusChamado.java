package com.pfc.thindesk.entity;

import java.text.Normalizer;

/**
 * Situações possíveis de um chamado.
 *
 * O banco já tinha registros gravados de formas diferentes ("Aberto", "ABERTO",
 * "EM_ANDAMENTO"...). O método {@link #normalizar(String)} converte qualquer uma
 * dessas variações para o valor oficial, e o {@link #getRotulo()} é o texto que
 * passa a ser gravado no MongoDB.
 */
public enum StatusChamado {

    ABERTO("Aberto"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado");

    private final String rotulo;

    StatusChamado(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Chamado ainda precisa de atenção (não foi encerrado nem cancelado). */
    public boolean isPendente() {
        return this == ABERTO || this == EM_ANDAMENTO;
    }

    /**
     * Converte um texto livre no status correspondente.
     * Retorna null quando o valor está vazio ou não é reconhecido.
     */
    public static StatusChamado normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String chave = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');

        return switch (chave) {
            case "ABERTO", "NOVO" -> ABERTO;
            case "EM_ANDAMENTO", "ANDAMENTO", "EM_ATENDIMENTO" -> EM_ANDAMENTO;
            case "CONCLUIDO", "FECHADO", "RESOLVIDO", "FINALIZADO" -> CONCLUIDO;
            case "CANCELADO" -> CANCELADO;
            default -> null;
        };
    }
}
