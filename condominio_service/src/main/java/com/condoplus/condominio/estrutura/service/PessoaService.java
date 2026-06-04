package com.condoplus.condominio.estrutura.service;

import com.condoplus.condominio.estrutura.client.IamClient;
import com.condoplus.condominio.estrutura.domain.Pessoa;
import com.condoplus.condominio.estrutura.dto.CredencialResponse;
import com.condoplus.condominio.estrutura.dto.CriarCredencialRequest;
import com.condoplus.condominio.estrutura.dto.NovaPessoaRequest;
import com.condoplus.condominio.estrutura.dto.PessoaResponse;
import com.condoplus.condominio.estrutura.repository.PessoaRepository;
import com.condoplus.condominio.exception.DocumentoJaExisteException;
import com.condoplus.condominio.exception.PessoaNaoEncontradaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Serviço responsável por orquestrar a lógica de negócio associada a Pessoas.
 * 
 * <p>Este serviço realiza o gerenciamento cadastral de moradores e proprietários,
 * integrando-se de forma resiliente com o {@link IamClient} para criação de 
 * credenciais no contexto de segurança.
 * 
 * <p>Anotações importantes:
 * <ul>
 *   <li>{@code @Service} — Declara esta classe como um componente de serviço gerenciado pelo Spring IoC, habilitando a injeção de dependências.</li>
 *   <li>{@code @RequiredArgsConstructor} — Gera pelo Lombok um construtor com argumentos para todos os campos {@code final}, eliminando a necessidade de Autowired explícito.</li>
 *   <li>{@code @Slf4j} — Injeta automaticamente um Logger SLF4J (Logback) sob o atributo {@code log} para registro detalhado das operações.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final IamClient iamClient;

    /**
     * Cadastra uma nova pessoa fisicamente e solicita a criação de sua credencial no IAM.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Garante atomicidade da operação. Se a persistência da pessoa local falhar, a transação é revertida (rollback) no banco PostgreSQL.</li>
     * </ul>
     * 
     * @param req DTO com os dados da pessoa e suas informações de acesso e permissão.
     * @return PessoaResponse com o ID gerado localmente e ID da credencial associada.
     * @throws DocumentoJaExisteException se já houver alguém cadastrado com o mesmo CPF.
     * @throws RuntimeException se a chamada ao IAM falhar após tentativas e fallback.
     */
    @Transactional
    public PessoaResponse cadastrar(NovaPessoaRequest req) {
        log.info("Iniciando cadastro de pessoa: nome={}, documento={}", req.nomeCompleto(), req.documento());

        if (pessoaRepository.existsByDocumento(req.documento())) {
            throw new DocumentoJaExisteException("Já existe uma pessoa cadastrada com o documento: " + req.documento());
        }

        UUID credencialId = null;
        try {
            CriarCredencialRequest credRequest = new CriarCredencialRequest(
                req.email(),
                req.senhaInicial(),
                req.role()
            );
            
            CredencialResponse credResponse = iamClient.criarCredencial(credRequest).join();
            credencialId = credResponse.id();
            log.info("Credencial criada com sucesso no IAM. credencialId={}", credencialId);
            
        } catch (Exception ex) {
            log.error("Erro ao chamar iam-service: {}", ex.getMessage());
            throw new RuntimeException("Falha no cadastro: " + ex.getCause().getMessage(), ex);
        }

        Pessoa pessoa = Pessoa.criar(
            credencialId,
            req.nomeCompleto(),
            req.documento(),
            req.telefone(),
            req.emailContato() != null ? req.emailContato() : req.email()
        );

        Pessoa salva = pessoaRepository.save(pessoa);
        log.info("Pessoa cadastrada com sucesso. id={}", salva.getId());
        
        return PessoaResponse.fromEntity(salva);
    }

    /**
     * Busca os dados de uma pessoa utilizando seu identificador único.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura no banco e evita locks desnecessários de escrita.</li>
     * </ul>
     * 
     * @param id Identificador único (UUID) da pessoa.
     * @return PessoaResponse contendo as informações da pessoa.
     * @throws PessoaNaoEncontradaException se a pessoa não for localizada.
     */
    @Transactional(readOnly = true)
    public PessoaResponse buscarPorId(UUID id) {
        return pessoaRepository.findById(id)
                .map(PessoaResponse::fromEntity)
                .orElseThrow(() -> new PessoaNaoEncontradaException(id));
    }

    /**
     * Busca os dados de uma pessoa utilizando o CPF cadastrado.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura no banco e evita locks desnecessários de escrita.</li>
     * </ul>
     * 
     * @param cpf CPF formatado da pessoa.
     * @return PessoaResponse correspondente.
     * @throws RuntimeException se nenhuma pessoa for localizada com o documento.
     */
    @Transactional(readOnly = true)
    public PessoaResponse buscarPorCpf(String cpf) {
        return pessoaRepository.findByDocumento(cpf)
                .map(PessoaResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Pessoa não encontrada com o documento: " + cpf));
    }

    /**
     * Lista todas as pessoas cadastradas no sistema.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura no banco e evita locks desnecessários de escrita.</li>
     * </ul>
     * 
     * @return Lista com os DTOs de todas as pessoas.
     */
    @Transactional(readOnly = true)
    public List<PessoaResponse> listarTodas() {
        return StreamSupport.stream(pessoaRepository.findAll().spliterator(), false)
                .map(PessoaResponse::fromEntity)
                .toList();
    }
}
