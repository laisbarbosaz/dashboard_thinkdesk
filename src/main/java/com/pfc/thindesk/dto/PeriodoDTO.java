package com.pfc.thindesk.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PeriodoDTO(
        String agrupamento,
        long totalAbertos,
        long totalConcluidos,
        List<PontoPeriodoDTO> pontos,
        LocalDateTime atualizadoEm) {
}
