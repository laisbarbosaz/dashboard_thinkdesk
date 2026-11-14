package com.pfc.thindesk.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.pfc.thindesk.entity.StatusChamado;
import com.pfc.thindesk.exception.ParametroInvalidoException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FiltroDashboardTest {

    @Test
    void parametrosVaziosViramSemFiltro() {
        FiltroDashboard f = FiltroDashboard.de("", null, "todos", " ", null, "");
        assertNull(f.dataInicio());
        assertNull(f.status());
        assertNull(f.tipo());
        assertFalse(f.temPeriodo());
    }

    @Test
    void lerDatasEStatus() {
        FiltroDashboard f = FiltroDashboard.de("2026-11-01", "2026-11-14", "em andamento", " Rede ", null, null);
        assertEquals(LocalDate.of(2026, 11, 1), f.dataInicio());
        assertEquals(StatusChamado.EM_ANDAMENTO, f.status());
        assertEquals("Rede", f.tipo());
        assertEquals(LocalDate.of(2026, 11, 15).atStartOfDay(), f.fimPeriodo());
    }

    @Test
    void dataEmFormatoErradoRetornaErroComNomeDoParametro() {
        ParametroInvalidoException e = assertThrows(ParametroInvalidoException.class,
                () -> FiltroDashboard.de("14/11/2026", null, null, null, null, null));
        assertEquals("dataInicio", e.getParametro());
    }

    @Test
    void inicioDepoisDoFimRetornaErro() {
        assertThrows(ParametroInvalidoException.class,
                () -> FiltroDashboard.de("2026-11-14", "2026-11-01", null, null, null, null));
    }

    @Test
    void statusDesconhecidoRetornaErro() {
        ParametroInvalidoException e = assertThrows(ParametroInvalidoException.class,
                () -> FiltroDashboard.de(null, null, "pendurado", null, null, null));
        assertEquals("status", e.getParametro());
    }

    @Test
    void agrupamento() {
        assertEquals(Agrupamento.DIA, Agrupamento.de(null));
        assertEquals(Agrupamento.MES, Agrupamento.de("mes"));
        assertThrows(ParametroInvalidoException.class, () -> Agrupamento.de("ano"));
    }
}
