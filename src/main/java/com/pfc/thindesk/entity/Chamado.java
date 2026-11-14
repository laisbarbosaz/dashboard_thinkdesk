package com.pfc.thindesk.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chamados")
public class Chamado {

    @Id
    private String id;
    private String descricao;
    @Indexed
    private String status;
    @Indexed
    private String tipo;
    @Indexed
    private String tecnico;
    private String usuario;

    // Campos adicionados para o dashboard administrativo
    private String prioridade;            // Baixa, Média, Alta
    private String consideracoesTecnicas; // anotações do técnico sobre o atendimento
    @Indexed
    private LocalDateTime dataAbertura;
    private LocalDateTime dataAtualizacao;
    @Indexed
    private LocalDateTime dataFechamento; // preenchida quando o chamado é concluído

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTecnico() {
        return tecnico;
    }

    public void setTecnico(String tecnico) {
        this.tecnico = tecnico;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(String prioridade) {
        this.prioridade = prioridade;
    }

    public String getConsideracoesTecnicas() {
        return consideracoesTecnicas;
    }

    public void setConsideracoesTecnicas(String consideracoesTecnicas) {
        this.consideracoesTecnicas = consideracoesTecnicas;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDateTime dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDateTime dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    /** Status já convertido para o enum (ou null se não reconhecido). */
    public StatusChamado statusNormalizado() {
        return StatusChamado.normalizar(status);
    }
}
