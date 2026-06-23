package com.condoplus.notificacao.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PreferenciaNaoEncontradaException.class)
    public ResponseEntity<ProblemDetail> lidarPreferenciaNaoEncontrada(
            PreferenciaNaoEncontradaException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(),
                "Preferência não encontrada", "/errors/preferencia-nao-encontrada");
    }

    @ExceptionHandler(NotificacaoNaoEncontradaException.class)
    public ResponseEntity<ProblemDetail> lidarNotificacaoNaoEncontrada(
            NotificacaoNaoEncontradaException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(),
                "Notificação não encontrada", "/errors/notificacao-nao-encontrada");
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ProblemDetail> lidarValidacao(WebExchangeBindException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                erros.put(err.getField(), err.getDefaultMessage()));
        ProblemDetail p = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Dados de entrada inválidos");
        p.setTitle("Validação falhou");
        p.setType(URI.create("https://condoplus.local"));
        p.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(p);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> lidarResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String detail = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return problem(status, detail, "Requisição inválida", "/errors/requisicao-invalida");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> lidarErroInesperado(Exception ex) {
        log.error("Erro inesperado no notificacao-service", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor", "Erro interno", "/errors/interno");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail,
                                                  String title, String typePath) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(title);
        p.setType(URI.create("https://condoplus.local" + typePath));
        return ResponseEntity.status(status).body(p);
    }
}
