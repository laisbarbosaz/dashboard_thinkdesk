package com.pfc.thindesk.dto;

import java.time.LocalDateTime;

/** Linha da tabela de chamados do dashboard. */
public record ChamadoListaDTO(
        String id,
        String descricao,
        String status,
        String tipo,
        String tecnico,
        String usuario,
        String prioridade,
        String consideracoesTecnicas,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        Double horasEmAberto,
        boolean foraDoPrazo) {
}
