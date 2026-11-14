package com.pfc.thindesk.dto;

import com.pfc.thindesk.exception.ParametroInvalidoException;

/** Granularidade do gráfico de chamados por período. */
public enum Agrupamento {
    DIA, SEMANA, MES;

    public static Agrupamento de(String valor) {
        if (valor == null || valor.isBlank()) {
            return DIA;
        }
        try {
            return Agrupamento.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParametroInvalidoException("agrupamento",
                    "Agrupamento '" + valor + "' inválido. Use: dia, semana ou mes.");
        }
    }
}
