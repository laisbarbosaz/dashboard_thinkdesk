package com.pfc.thindesk.exception;

/** Registro não encontrado. Vira resposta HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
