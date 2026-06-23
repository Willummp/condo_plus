package com.condoplus.condominio.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UnidadeNaoEncontradaException.class)
    public ResponseEntity<ProblemDetail> handleUnidadeNaoEncontrada(UnidadeNaoEncontradaException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(),
                "Unidade não encontrada", "/errors/unidade-nao-encontrada");
    }

    @ExceptionHandler(PessoaNaoEncontradaException.class)
    public ResponseEntity<ProblemDetail> handlePessoaNaoEncontrada(PessoaNaoEncontradaException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(),
                "Pessoa não encontrada", "/errors/pessoa-nao-encontrada");
    }

    @ExceptionHandler(AreaComumNaoEncontradaException.class)
    public ResponseEntity<ProblemDetail> handleAreaNaoEncontrada(AreaComumNaoEncontradaException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(),
                "Área comum não encontrada", "/errors/area-comum-nao-encontrada");
    }

    @ExceptionHandler(DocumentoJaExisteException.class)
    public ResponseEntity<ProblemDetail> handleDocumentoDuplicado(DocumentoJaExisteException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(),
                "Conflito de documento", "/errors/documento-ja-existe");
    }

    @ExceptionHandler(ConflitoReservaException.class)
    public ResponseEntity<ProblemDetail> handleConflitoReserva(ConflitoReservaException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(),
                "Conflito de reserva", "/errors/conflito-reserva");
    }

    @ExceptionHandler(AreaComumIndisponivelException.class)
    public ResponseEntity<ProblemDetail> handleAreaIndisponivel(AreaComumIndisponivelException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "Área comum indisponível", "/errors/area-indisponivel");
    }

    
    @ExceptionHandler({ConcurrencyFailureException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ProblemDetail> handleConcorrencia(Exception ex) {
        log.info("Conflito de concorrência detectado. message={}", ex.getMessage());
        return problem(HttpStatus.CONFLICT,
                "Recurso foi alterado por outra operação. Tente novamente.",
                "Conflito de concorrência", "/errors/concorrencia");
    }

    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(err -> erros.put(err.getField(), err.getDefaultMessage()));

        ProblemDetail p = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Dados de entrada inválidos");
        p.setTitle("Validação falhou");
        p.setType(URI.create("https://condoplus.local/errors/validacao"));
        p.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(p);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAcessoNegado(AuthorizationDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Acesso negado", "Acesso negado", "/errors/acesso-negado");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleErroInesperado(Exception ex) {
        log.error("Erro inesperado no condominio-service", ex);
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
