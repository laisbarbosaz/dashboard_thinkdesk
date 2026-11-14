package com.pfc.thindesk.controller;

import com.pfc.thindesk.dto.ErroApiDTO;
import com.pfc.thindesk.exception.ParametroInvalidoException;
import com.pfc.thindesk.exception.RecursoNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Padroniza as respostas de erro dos controllers REST (/api/**). */
@RestControllerAdvice(assignableTypes = {DashboardApiController.class, ChamadoController.class})
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<ErroApiDTO> parametroInvalido(ParametroInvalidoException e, HttpServletRequest req) {
        return erro(HttpStatus.BAD_REQUEST, e.getMessage(), e.getParametro(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroApiDTO> tipoInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        String msg = "Valor '" + e.getValue() + "' inválido para o parâmetro '" + e.getName() + "'.";
        return erro(HttpStatus.BAD_REQUEST, msg, e.getName(), req);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroApiDTO> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest req) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage(), null, req);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErroApiDTO> bancoIndisponivel(DataAccessException e, HttpServletRequest req) {
        log.error("Falha ao acessar o MongoDB", e);
        return erro(HttpStatus.SERVICE_UNAVAILABLE,
                "Não foi possível consultar o banco de dados. Tente novamente em instantes.", null, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroApiDTO> inesperado(Exception e, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisição.", null, req);
    }

    private ResponseEntity<ErroApiDTO> erro(HttpStatus status, String mensagem, String parametro,
                                            HttpServletRequest req) {
        ErroApiDTO corpo = new ErroApiDTO(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                mensagem, parametro, req.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
