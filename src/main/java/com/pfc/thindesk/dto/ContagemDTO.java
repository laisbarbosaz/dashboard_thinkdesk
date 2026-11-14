package com.pfc.thindesk.dto;

/** Quantidade por categoria (status, tipo...) com o percentual sobre o total. */
public record ContagemDTO(String nome, long quantidade, double percentual) {
}
