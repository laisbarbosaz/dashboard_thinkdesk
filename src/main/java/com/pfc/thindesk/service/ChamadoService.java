package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.StatusChamado;
import com.pfc.thindesk.exception.RecursoNaoEncontradoException;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChamadoService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    public Chamado criarChamado(Chamado chamado) {
        LocalDateTime agora = LocalDateTime.now();
        StatusChamado status = StatusChamado.normalizar(chamado.getStatus());
        chamado.setStatus((status != null ? status : StatusChamado.ABERTO).getRotulo());
        if (chamado.getDataAbertura() == null) {
            chamado.setDataAbertura(agora);
        }
        chamado.setDataAtualizacao(agora);
        ajustarDataFechamento(chamado, agora);
        return chamadoRepository.save(chamado);
    }

    public List<Chamado> listarChamados() {
        return chamadoRepository.findAll();
    }

    public Chamado buscarPorId(String id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado com id: " + id));
    }

    public Chamado atualizarChamado(String id, Chamado chamadoAtualizado) {
        Chamado chamado = buscarPorId(id);
        LocalDateTime agora = LocalDateTime.now();

        chamado.setDescricao(chamadoAtualizado.getDescricao());
        StatusChamado status = StatusChamado.normalizar(chamadoAtualizado.getStatus());
        chamado.setStatus(status != null ? status.getRotulo() : chamadoAtualizado.getStatus());
        chamado.setTipo(chamadoAtualizado.getTipo());
        chamado.setTecnico(chamadoAtualizado.getTecnico());
        chamado.setUsuario(chamadoAtualizado.getUsuario());

        // Campos novos só são sobrescritos quando enviados, para não apagar dados existentes
        if (chamadoAtualizado.getPrioridade() != null) {
            chamado.setPrioridade(chamadoAtualizado.getPrioridade());
        }
        if (chamadoAtualizado.getConsideracoesTecnicas() != null) {
            chamado.setConsideracoesTecnicas(chamadoAtualizado.getConsideracoesTecnicas());
        }

        chamado.setDataAtualizacao(agora);
        ajustarDataFechamento(chamado, agora);
        return chamadoRepository.save(chamado);
    }

    /** Registra a data de fechamento ao concluir e limpa se o chamado for reaberto. */
    private void ajustarDataFechamento(Chamado chamado, LocalDateTime agora) {
        StatusChamado status = chamado.statusNormalizado();
        if (status == StatusChamado.CONCLUIDO) {
            if (chamado.getDataFechamento() == null) {
                chamado.setDataFechamento(agora);
            }
        } else if (status != StatusChamado.CANCELADO) {
            chamado.setDataFechamento(null);
        }
    }
}
