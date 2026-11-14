package com.pfc.thindesk.dto;

import java.time.LocalDateTime;

/** Indicadores gerais (RF01). */
public record ResumoDTO(
        long total,
        long abertos,
        long emAndamento,
        long concluidos,
        long cancelados,
        long pendentes,
        long pendentesForaDoPrazo,
        int prazoHoras,
        Double taxaResolucao,
        Double tempoMedioResolucaoHoras,
        long semStatusDefinido,
        LocalDateTime atualizadoEm) {
}
