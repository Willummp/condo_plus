package com.condoplus.condominio.estrutura.service;

import com.condoplus.condominio.estrutura.domain.Veiculo;
import com.condoplus.condominio.estrutura.dto.NovoVeiculoRequest;
import com.condoplus.condominio.estrutura.dto.VeiculoResponse;
import com.condoplus.condominio.estrutura.repository.UnidadeRepository;
import com.condoplus.condominio.estrutura.repository.VeiculoRepository;
import com.condoplus.condominio.exception.UnidadeNaoEncontradaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Serviço responsável por orquestrar a lógica de negócio associada a Veículos no condomínio.
 * 
 * <p>Este serviço gerencia o cadastro e associação de automóveis permitidos a cada unidade residencial,
 * garantindo a validação de placa única e o controle de acesso às garagens do condomínio.
 * 
 * <p>Anotações importantes:
 * <ul>
 *   <li>{@code @Service} — Registra a classe como componente de serviço Spring para injeção de dependências.</li>
 *   <li>{@code @RequiredArgsConstructor} — Cria pelo Lombok o construtor contendo os campos {@code final}.</li>
 *   <li>{@code @Slf4j} — Injeta automaticamente o Logger SLF4J para registro das operações de tráfego.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VeiculoService {

    /**
     * Repositório de dados para persistência e busca de veículos.
     */
    private final VeiculoRepository veiculoRepository;

    /**
     * Repositório de dados para validação e busca de unidades residenciais.
     */
    private final UnidadeRepository unidadeRepository;

    /**
     * Cadastra um novo veículo atrelado a uma unidade residencial no condomínio.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Habilita suporte transacional atômico de escrita.</li>
     * </ul>
     * 
     * @param req DTO contendo a placa, modelo, cor e ID da unidade residencial.
     * @return Um {@link VeiculoResponse} estruturado com os dados cadastrados.
     * @throws UnidadeNaoEncontradaException se a unidade residencial informada não existir.
     * @throws RuntimeException se a placa do veículo (após normalização) já constar cadastrada no sistema.
     */
    @Transactional
    public VeiculoResponse cadastrar(NovoVeiculoRequest req) {
        log.info("Cadastrando veículo com placa: {} para a unidade: {}", req.placa(), req.unidadeId());

        if (!unidadeRepository.existsById(req.unidadeId())) {
            throw new UnidadeNaoEncontradaException(req.unidadeId());
        }

        String placaNormalizada = req.placa().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (veiculoRepository.existsByPlaca(placaNormalizada)) {
            throw new RuntimeException("Já existe um veículo cadastrado com a placa: " + req.placa());
        }

        Veiculo veiculo = Veiculo.criar(req.placa(), req.modelo(), req.cor(), req.unidadeId());
        Veiculo salvo = veiculoRepository.save(veiculo);
        log.info("Veículo cadastrado com sucesso. id={}", salvo.getId());

        return VeiculoResponse.fromEntity(salvo);
    }

    /**
     * Busca as informações cadastrais de um veículo específico por seu identificador único.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura.</li>
     * </ul>
     * 
     * @param id ID único do veículo buscado.
     * @return Um DTO {@link VeiculoResponse} correspondente ao veículo localizado.
     * @throws RuntimeException se o veículo correspondente ao ID não for localizado.
     */
    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorId(UUID id) {
        return veiculoRepository.findById(id)
                .map(VeiculoResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado com o ID: " + id));
    }

    /**
     * Retorna a lista contendo as informações de todos os veículos cadastrados no condomínio.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura.</li>
     * </ul>
     * 
     * @return Lista com os DTOs de todos os veículos cadastrados.
     */
    @Transactional(readOnly = true)
    public List<VeiculoResponse> listarTodos() {
        return StreamSupport.stream(veiculoRepository.findAll().spliterator(), false)
                .map(VeiculoResponse::fromEntity)
                .toList();
    }

    /**
     * Retorna a lista de veículos ativos atrelados a uma unidade residencial específica.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Habilita otimizações de leitura.</li>
     * </ul>
     * 
     * @param unidadeId ID da unidade residencial procurada.
     * @return Lista contendo os DTOs de veículos ativos atrelados à unidade.
     */
    @Transactional(readOnly = true)
    public List<VeiculoResponse> listarPorUnidade(UUID unidadeId) {
        return veiculoRepository.findAtivosByUnidade(unidadeId).stream()
                .map(VeiculoResponse::fromEntity)
                .toList();
    }

    /**
     * Desativa logicamente (soft delete) o veículo do condomínio, retirando sua autorização de garagem.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Executa a alteração em transação de escrita atômica.</li>
     * </ul>
     * 
     * @param id ID do veículo a ser desativado.
     * @throws RuntimeException se o veículo correspondente ao ID não for localizado.
     */
    @Transactional
    public void desativar(UUID id) {
        log.info("Desativando veículo: {}", id);
        Veiculo veiculo = veiculoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado com o ID: " + id));
        veiculo.desativar();
        veiculoRepository.save(veiculo);
    }
}
