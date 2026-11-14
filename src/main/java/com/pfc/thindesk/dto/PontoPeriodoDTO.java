package com.pfc.thindesk.dto;

import java.time.LocalDate;

/** Um ponto do gráfico de chamados por período (RF02). */
public record PontoPeriodoDTO(
        LocalDate inicio,
        String rotulo,
        long abertos,
        long concluidos) {
}
