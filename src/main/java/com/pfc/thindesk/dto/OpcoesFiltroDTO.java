package com.pfc.thindesk.dto;

import java.util.List;

/** Valores disponíveis para preencher os campos de filtro da tela. */
public record OpcoesFiltroDTO(
        List<String> status,
        List<String> tipos,
        List<String> tecnicos,
        List<String> prioridades) {
}
