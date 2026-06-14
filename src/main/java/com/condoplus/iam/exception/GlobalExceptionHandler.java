package	com.condoplus.iam.exception;

import	lombok.extern.slf4j.Slf4j;
import	org.springframework.http.HttpStatus;
import	org.springframework.http.ProblemDetail;
import	org.springframework.http.ResponseEntity;
import	org.springframework.web.bind.MethodArgumentNotValidException;
import	org.springframework.web.bind.annotation.ExceptionHandler;
import	org.springframework.web.bind.annotation.RestControllerAdvice;
import	java.net.URI;
import	java.util.HashMap;
import	java.util.Map;

@RestControllerAdvice
@Slf4j
public	class	GlobalExceptionHandler	{
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public	ResponseEntity<ProblemDetail>	lidarCredenciaisInvalidas(
            CredenciaisInvalidasException	ex)	{
        //	Não	logamos	detalhes	—	já	foi	logado	dentro	do	AutenticacaoService
        ProblemDetail	problem	=	ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Credenciais	inválidas"
        );
        problem.setType(URI.create("https://condoplus.local/errors/credenciais-invalidas"));
        problem.setTitle("Credenciais	inválidas");
        return	ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
    @ExceptionHandler(CredencialBloqueadaException.class)
    public	ResponseEntity<ProblemDetail>	lidarCredencialBloqueada(
            CredencialBloqueadaException	ex)	{
        ProblemDetail	problem	=	ProblemDetail.forStatusAndDetail(
                HttpStatus.LOCKED,
                "Credencial	bloqueada"
        );
        problem.setType(URI.create("https://condoplus.local/errors/credencial-bloqueada"));
        problem.setTitle("Credencial	bloqueada");
        return	ResponseEntity.status(HttpStatus.LOCKED).body(problem);
    }
    @ExceptionHandler(EmailJaExisteException.class)
    public	ResponseEntity<ProblemDetail>	lidarEmailJaExiste(EmailJaExisteException	ex)	{
        log.info("Tentativa	de	cadastro	com	e-mail	já	existente.");
        ProblemDetail	problem	=	ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "E-mail	já	cadastrado"
        );
        problem.setType(URI.create("https://condoplus.local/errors/email-ja-existe"));
        problem.setTitle("Conflito	ao	criar	credencial");
        return	ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
    @ExceptionHandler(CredencialNaoEncontradaException.class)
    public	ResponseEntity<ProblemDetail>	lidarNaoEncontrada(
            CredencialNaoEncontradaException	ex)	{
        ProblemDetail	problem	=	ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setType(URI.create("https://condoplus.local/errors/credencial-nao-encontrada"));
        problem.setTitle("Credencial	não	encontrada");
        return	ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public	ResponseEntity<ProblemDetail>	lidarValidacao(
            MethodArgumentNotValidException	ex)	{
        Map<String,	String>	erros	=	new	HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err	->
                erros.put(err.getField(),	err.getDefaultMessage())
        );
        ProblemDetail	problem	=	ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Dados	de	entrada	inválidos"
        );
        problem.setType(URI.create("https://condoplus.local/errors/validacao"));
        problem.setTitle("Validação	falhou");
        problem.setProperty("erros",	erros);
        return	ResponseEntity.badRequest().body(problem);
    }
    @ExceptionHandler(Exception.class)
    public	ResponseEntity<ProblemDetail>	lidarErroInesperado(Exception	ex)	{
        log.error("Erro	inesperado",	ex);
        ProblemDetail	problem	=	ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro	inesperado"
        );
        problem.setType(URI.create("https://condoplus.local/errors/interno"));
        problem.setTitle("Erro	interno");
        return	ResponseEntity.internalServerError().body(problem);
    }
}