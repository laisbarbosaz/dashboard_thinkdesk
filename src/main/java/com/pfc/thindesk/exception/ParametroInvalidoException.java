package com.pfc.thindesk.exception;

/** Parâmetro de consulta inválido. Vira resposta HTTP 400. */
public class ParametroInvalidoException extends RuntimeException {

    private final String parametro;

    public ParametroInvalidoException(String parametro, String mensagem) {
        super(mensagem);
        this.parametro = parametro;
    }

    public String getParametro() {
        return parametro;
    }
}
