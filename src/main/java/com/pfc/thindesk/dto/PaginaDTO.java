package com.pfc.thindesk.dto;

import java.util.List;

public record PaginaDTO<T>(
        List<T> itens,
        int pagina,
        int tamanho,
        long totalItens,
        int totalPaginas) {
}
