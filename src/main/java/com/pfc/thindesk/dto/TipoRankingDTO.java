package com.pfc.thindesk.dto;

/** Posição de um tipo de chamado no ranking (RF04). */
public record TipoRankingDTO(
        int posicao,
        String tipo,
        long total,
        long concluidos,
        double percentual) {
}
