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

@Service
@RequiredArgsConstructor
@Slf4j
public class VeiculoService {

    
    private final VeiculoRepository veiculoRepository;

    
    private final UnidadeRepository unidadeRepository;

    
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

    
    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorId(UUID id) {
        return veiculoRepository.findById(id)
                .map(VeiculoResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado com o ID: " + id));
    }

    
    @Transactional(readOnly = true)
    public List<VeiculoResponse> listarTodos() {
        return StreamSupport.stream(veiculoRepository.findAll().spliterator(), false)
                .map(VeiculoResponse::fromEntity)
                .toList();
    }

    
    @Transactional(readOnly = true)
    public List<VeiculoResponse> listarPorUnidade(UUID unidadeId) {
        return veiculoRepository.findAtivosByUnidade(unidadeId).stream()
                .map(VeiculoResponse::fromEntity)
                .toList();
    }

    
    @Transactional
    public void desativar(UUID id) {
        log.info("Desativando veículo: {}", id);
        Veiculo veiculo = veiculoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado com o ID: " + id));
        veiculo.desativar();
        veiculoRepository.save(veiculo);
    }
}
