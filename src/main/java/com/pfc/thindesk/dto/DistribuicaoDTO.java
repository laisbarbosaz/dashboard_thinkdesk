package com.pfc.thindesk.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DistribuicaoDTO(long total, List<ContagemDTO> itens, LocalDateTime atualizadoEm) {
}
