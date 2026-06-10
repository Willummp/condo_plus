package com.condoplus.condominio.estrutura.service;

import com.condoplus.condominio.estrutura.domain.CargoFuncionario;
import com.condoplus.condominio.estrutura.domain.Funcionario;
import com.condoplus.condominio.estrutura.dto.FuncionarioResponse;
import com.condoplus.condominio.estrutura.dto.NovoFuncionarioRequest;
import com.condoplus.condominio.estrutura.repository.FuncionarioRepository;
import com.condoplus.condominio.estrutura.repository.PessoaRepository;
import com.condoplus.condominio.exception.PessoaNaoEncontradaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Serviço responsável por orquestrar a lógica de negócio associada a Funcionários do condomínio.
 * 
 * <p>Este serviço gerencia o ciclo de vida trabalhista dos prestadores de serviço operacionais,
 * incluindo sua admissão cadastral e demissão lógica (soft delete) para garantir a integridade dos logs
 * históricos do condomínio.
 * 
 * <p>Anotações importantes:
 * <ul>
 *   <li>{@code @Service} — Declara esta classe como um componente de serviço Spring gerenciado para fins de injeção de dependências.</li>
 *   <li>{@code @RequiredArgsConstructor} — Gera automaticamente pelo Lombok o construtor contendo os campos {@code final}.</li>
 *   <li>{@code @Slf4j} — Injeta automaticamente o Logger SLF4J para registro detalhado das admissões e desligamentos.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FuncionarioService {

    /**
     * Repositório de dados para persistência e consultas operacionais de funcionários.
     */
    private final FuncionarioRepository funcionarioRepository;

    /**
     * Repositório de dados para verificação cadastral de pessoas físicas.
     */
    private final PessoaRepository pessoaRepository;

    /**
     * Registra a admissão trabalhista de uma pessoa física como funcionária do condomínio.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Garante que a operação ocorra sob uma transação atômica. Qualquer exceção lançada
     *   provocará o rollback automático no banco de dados.</li>
     * </ul>
     * 
     * @param req DTO contendo o ID da pessoa física cadastrada, o cargo operacional e a data de admissão.
     * @return Um {@link FuncionarioResponse} com as informações estruturadas do novo funcionário.
     * @throws PessoaNaoEncontradaException caso o {@code pessoaId} informado não exista no sistema.
     * @throws RuntimeException caso a pessoa informada já seja uma funcionária ativa com outro cargo.
     */
    @Transactional
    public FuncionarioResponse admitir(NovoFuncionarioRequest req) {
        log.info("Admitindo funcionário: pessoaId={}, cargo={}", req.pessoaId(), req.cargo());

        if (!pessoaRepository.existsById(req.pessoaId())) {
            throw new PessoaNaoEncontradaException(req.pessoaId());
        }

        funcionarioRepository.findByPessoaId(req.pessoaId()).ifPresent(f -> {
            if (f.isAtivo()) {
                throw new RuntimeException("Esta pessoa já é uma funcionária ativa com cargo: " + f.getCargo());
            }
        });

        Funcionario funcionario = Funcionario.criar(req.pessoaId(), req.cargo(), req.dataAdmissao());
        Funcionario salvo = funcionarioRepository.save(funcionario);
        log.info("Funcionário admitido com sucesso. id={}", salvo.getId());

        return FuncionarioResponse.fromEntity(salvo);
    }

    /**
     * Busca os dados de admissão de um funcionário por seu identificador único.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura no banco de dados, omitindo locks de escrita.</li>
     * </ul>
     * 
     * @param id ID do funcionário a ser buscado.
     * @return Um DTO {@link FuncionarioResponse} correspondente ao funcionário localizado.
     * @throws RuntimeException se o funcionário não for localizado.
     */
    @Transactional(readOnly = true)
    public FuncionarioResponse buscarPorId(UUID id) {
        return funcionarioRepository.findById(id)
                .map(FuncionarioResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado com o ID: " + id));
    }

    /**
     * Lista todos os funcionários registrados no sistema, com opção de filtrar apenas por ativos.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura no banco de dados.</li>
     * </ul>
     * 
     * @param apenasAtivos Se {@code true}, retorna apenas os que estão em pleno exercício; se {@code false}, inclui demitidos.
     * @return Lista contendo os DTOs de todos os funcionários correspondentes ao filtro.
     */
    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarTodos(boolean apenasAtivos) {
        if (apenasAtivos) {
            return funcionarioRepository.findAllAtivos().stream()
                    .map(FuncionarioResponse::fromEntity)
                    .toList();
        }
        return StreamSupport.stream(funcionarioRepository.findAll().spliterator(), false)
                .map(FuncionarioResponse::fromEntity)
                .toList();
    }

    /**
     * Lista todos os funcionários ativos que desempenham um determinado cargo.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura no banco de dados.</li>
     * </ul>
     * 
     * @param cargo O {@link CargoFuncionario} a ser filtrado.
     * @return Lista de funcionários ativos exercendo o cargo correspondente.
     */
    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarPorCargo(CargoFuncionario cargo) {
        return funcionarioRepository.findAtivosByCargo(cargo).stream()
                .map(FuncionarioResponse::fromEntity)
                .toList();
    }

    /**
     * Efetua o desligamento (demissão lógica/soft delete) de um funcionário do condomínio.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Executa a alteração em transação de escrita atômica.</li>
     * </ul>
     * 
     * @param id ID do funcionário a ser desligado.
     * @param dataDesligamento Data oficial de desligamento (se nula, assume o dia corrente).
     * @throws RuntimeException se o funcionário correspondente ao ID não for localizado.
     */
    @Transactional
    public void desligar(UUID id, LocalDate dataDesligamento) {
        log.info("Desligando funcionário: id={}, dataDesligamento={}", id, dataDesligamento);
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado com o ID: " + id));
        
        funcionario.desligar(dataDesligamento != null ? dataDesligamento : LocalDate.now());
        funcionarioRepository.save(funcionario);
        log.info("Funcionário desligado com sucesso. id={}", id);
    }
}
