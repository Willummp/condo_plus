package com.condoplus.condominio.estrutura.service;

import com.condoplus.condominio.estrutura.client.IamClient;
import com.condoplus.condominio.estrutura.domain.Pessoa;
import com.condoplus.condominio.estrutura.dto.CredencialResponse;
import com.condoplus.condominio.estrutura.dto.NovaPessoaRequest;
import com.condoplus.condominio.estrutura.dto.PessoaResponse;
import com.condoplus.condominio.estrutura.repository.PessoaRepository;
import com.condoplus.condominio.exception.DocumentoJaExisteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PessoaServiceTest {

    @Mock
    private PessoaRepository pessoaRepository;

    @Mock
    private IamClient iamClient;

    @InjectMocks
    private PessoaService pessoaService;

    @Test
    @DisplayName("Deve cadastrar uma pessoa com sucesso quando o documento é único e o IAM responde com sucesso")
    void cadastrarPessoaSucesso() {

        NovaPessoaRequest request = new NovaPessoaRequest(
                "Lucas Ferreira",
                "lucas@email.com",
                "senha123",
                "12345678909",
                "21999998888",
                "lucas.contato@email.com",
                "MORADOR"
        );

        UUID generatedCredencialId = UUID.randomUUID();
        CredencialResponse iamResponse = new CredencialResponse(generatedCredencialId, request.email(), request.role());
        
        Pessoa mockPessoaSalva = Pessoa.criar(
                generatedCredencialId,
                request.nomeCompleto(),
                request.documento(),
                request.telefone(),
                request.emailContato()
        );
        mockPessoaSalva.setId(UUID.randomUUID());

        when(pessoaRepository.existsByDocumento(request.documento())).thenReturn(false);
        when(iamClient.criarCredencial(any())).thenReturn(Mono.just(iamResponse));
        when(pessoaRepository.save(any(Pessoa.class))).thenReturn(mockPessoaSalva);

        PessoaResponse response = pessoaService.cadastrar(request);

        assertNotNull(response);
        assertEquals(mockPessoaSalva.getId(), response.id());
        assertEquals(generatedCredencialId, response.credencialId());
        assertEquals(request.nomeCompleto(), response.nomeCompleto());
        verify(pessoaRepository, times(1)).existsByDocumento(request.documento());
        verify(iamClient, times(1)).criarCredencial(any());
        verify(pessoaRepository, times(1)).save(any(Pessoa.class));
    }

    @Test
    @DisplayName("Deve lançar DocumentoJaExisteException quando o documento (CPF) já estiver cadastrado")
    void cadastrarPessoaFalhaCpfExistente() {

        NovaPessoaRequest request = new NovaPessoaRequest(
                "Lucas Ferreira",
                "lucas@email.com",
                "senha123",
                "12345678909",
                "21999998888",
                "lucas.contato@email.com",
                "MORADOR"
        );

        when(pessoaRepository.existsByDocumento(request.documento())).thenReturn(true);

        assertThrows(DocumentoJaExisteException.class, () -> pessoaService.cadastrar(request));
        verify(pessoaRepository, times(1)).existsByDocumento(request.documento());
        verifyNoInteractions(iamClient);
        verify(pessoaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar e repassar o erro se o IamClient falhar (simulando Circuit Breaker ativado)")
    void cadastrarPessoaFalhaIamIndisponivel() {

        NovaPessoaRequest request = new NovaPessoaRequest(
                "Lucas Ferreira",
                "lucas@email.com",
                "senha123",
                "12345678909",
                "21999998888",
                "lucas.contato@email.com",
                "MORADOR"
        );

        when(pessoaRepository.existsByDocumento(request.documento())).thenReturn(false);
        
        RuntimeException iamException = new RuntimeException("Serviço de Autenticação (IAM) temporariamente indisponível.");
        when(iamClient.criarCredencial(any())).thenReturn(Mono.error(iamException));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> pessoaService.cadastrar(request));
        assertTrue(thrown.getMessage().contains("Falha no cadastro"));
        
        verify(pessoaRepository, times(1)).existsByDocumento(request.documento());
        verify(iamClient, times(1)).criarCredencial(any());
        verify(pessoaRepository, never()).save(any());
    }
}
