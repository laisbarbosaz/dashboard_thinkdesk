package com.pfc.thindesk.dto;

import java.time.LocalDateTime;

/** Corpo padrão das respostas de erro da API. */
public record ErroApiDTO(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String parametro,
        String caminho) {
}
