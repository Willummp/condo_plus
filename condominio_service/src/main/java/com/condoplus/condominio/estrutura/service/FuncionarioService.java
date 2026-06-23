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

@Service
@RequiredArgsConstructor
@Slf4j
public class FuncionarioService {

    
    private final FuncionarioRepository funcionarioRepository;

    
    private final PessoaRepository pessoaRepository;

    
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

    
    @Transactional(readOnly = true)
    public FuncionarioResponse buscarPorId(UUID id) {
        return funcionarioRepository.findById(id)
                .map(FuncionarioResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado com o ID: " + id));
    }

    
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

    
    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarPorCargo(CargoFuncionario cargo) {
        return funcionarioRepository.findAtivosByCargo(cargo).stream()
                .map(FuncionarioResponse::fromEntity)
                .toList();
    }

    
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
