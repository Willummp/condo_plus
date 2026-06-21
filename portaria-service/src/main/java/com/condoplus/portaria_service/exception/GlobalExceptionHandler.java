package com.condoplus.portaria_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({VisitanteNaoEncontradoException.class,
            EncomendaNaoEncontradaException.class})
    public ResponseEntity<ProblemDetail> lidarNaoEncontrado(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(),
                "Recurso não encontrado", "/errors/nao-encontrado");
    }

    @ExceptionHandler(AutorizacaoNegadaException.class)
    public ResponseEntity<ProblemDetail> lidarAutorizacaoNegada(AutorizacaoNegadaException ex) {
        log.warn("Autorização negada: {}", ex.getMessage());
        return problem(HttpStatus.FORBIDDEN, ex.getMessage(),
                "Autorização negada", "/errors/autorizacao-negada");
    }

    @ExceptionHandler(VisitanteNaoAtivoException.class)
    public ResponseEntity<ProblemDetail> lidarVisitanteInativo(VisitanteNaoAtivoException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "Visitante não ativo", "/errors/visitante-inativo");
    }

    @ExceptionHandler(EncomendaJaRetiradaException.class)
    public ResponseEntity<ProblemDetail> lidarEncomendaJaRetirada(EncomendaJaRetiradaException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(),
                "Estado inválido", "/errors/encomenda-ja-retirada");
    }

    @ExceptionHandler(PlacaNaoCadastradaException.class)
    public ResponseEntity<ProblemDetail> lidarPlacaNaoCadastrada(PlacaNaoCadastradaException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "Placa não cadastrada", "/errors/placa-nao-cadastrada");
    }

    @ExceptionHandler(PlacaNaoPertenceUnidadeException.class)
    public ResponseEntity<ProblemDetail> lidarPlacaNaoPertenceUnidade(PlacaNaoPertenceUnidadeException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "Placa não vinculada à unidade", "/errors/placa-unidade");
    }

    @ExceptionHandler(CondominioServiceIndisponivelException.class)
    public ResponseEntity<ProblemDetail> lidarCondominioIndisponivel(
            CondominioServiceIndisponivelException ex) {
        log.error("condominio-service indisponível durante operação.", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(),
                "Serviço indisponível", "/errors/condominio-indisponivel");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> lidarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> erros.put(err.getField(), err.getDefaultMessage()));

        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        p.setTitle("Validação falhou");
        p.setType(URI.create("https://condoplus.local/errors/validacao"));
        p.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(p);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> lidarErroInesperado(Exception ex) {
        log.error("Erro inesperado no portaria-service", ex);
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
