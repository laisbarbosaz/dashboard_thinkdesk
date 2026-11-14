package com.pfc.thindesk.dto;

/** Desempenho de um técnico no período filtrado. */
public record TecnicoDTO(
        String tecnico,
        long atribuidos,
        long concluidos,
        long pendentes,
        Double tempoMedioResolucaoHoras) {
}
