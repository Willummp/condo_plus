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
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final IamClient iamClient;

    
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
                Set.of(req.role())
            );
            
            CredencialResponse credResponse = iamClient.criarCredencial(credRequest).block();
            if (credResponse == null) {
                throw new RuntimeException("Resposta nula recebida do IAM");
            }
            credencialId = credResponse.id();
            log.info("Credencial criada com sucesso no IAM. credencialId={}", credencialId);
            
        } catch (Exception ex) {
            log.error("Erro ao chamar iam-service: {}", ex.getMessage());
            throw new RuntimeException("Falha no cadastro: " + ex.getMessage(), ex);
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

    
    @Transactional(readOnly = true)
    public PessoaResponse buscarPorId(UUID id) {
        return pessoaRepository.findById(id)
                .map(PessoaResponse::fromEntity)
                .orElseThrow(() -> new PessoaNaoEncontradaException(id));
    }

    
    @Transactional(readOnly = true)
    public PessoaResponse buscarPorCpf(String cpf) {
        return pessoaRepository.findByDocumento(cpf)
                .map(PessoaResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Pessoa não encontrada com o documento: " + cpf));
    }

    
    @Transactional(readOnly = true)
    public List<PessoaResponse> listarTodas() {
        return StreamSupport.stream(pessoaRepository.findAll().spliterator(), false)
                .map(PessoaResponse::fromEntity)
                .toList();
    }
}
