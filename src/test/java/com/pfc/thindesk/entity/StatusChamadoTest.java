package com.pfc.thindesk.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StatusChamadoTest {

    @Test
    void reconheceVariacoesUsadasNoBanco() {
        assertEquals(StatusChamado.ABERTO, StatusChamado.normalizar("Aberto"));
        assertEquals(StatusChamado.ABERTO, StatusChamado.normalizar("ABERTO"));
        assertEquals(StatusChamado.EM_ANDAMENTO, StatusChamado.normalizar("EM_ANDAMENTO"));
        assertEquals(StatusChamado.EM_ANDAMENTO, StatusChamado.normalizar("Em andamento"));
        assertEquals(StatusChamado.CONCLUIDO, StatusChamado.normalizar("Concluído"));
        assertEquals(StatusChamado.CONCLUIDO, StatusChamado.normalizar(" concluido "));
        assertEquals(StatusChamado.CONCLUIDO, StatusChamado.normalizar("Fechado"));
        assertEquals(StatusChamado.CANCELADO, StatusChamado.normalizar("cancelado"));
    }

    @Test
    void valorDesconhecidoOuVazioRetornaNull() {
        assertNull(StatusChamado.normalizar(null));
        assertNull(StatusChamado.normalizar(""));
        assertNull(StatusChamado.normalizar("xyz"));
    }

    @Test
    void pendentes() {
        assertTrue(StatusChamado.ABERTO.isPendente());
        assertTrue(StatusChamado.EM_ANDAMENTO.isPendente());
        assertFalse(StatusChamado.CONCLUIDO.isPendente());
        assertFalse(StatusChamado.CANCELADO.isPendente());
    }
}
