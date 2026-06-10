package com.condoplus.condominio.consumer;

import com.condoplus.condominio.event.CredencialCriadaEvent;
import com.condoplus.condominio.estrutura.domain.Pessoa;
import com.condoplus.condominio.estrutura.repository.PessoaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CredencialCriadaConsumer {

    private final PessoaRepository pessoaRepository;

    @KafkaListener(topics = "credenciais.criadas", groupId = "condominio-group")
    public void consumir(CredencialCriadaEvent event) {
        // Restaurar CorrelationId para rastreamento nos logs
        if (event.correlationId() != null) {
            MDC.put("correlationId", event.correlationId());
        }

        try {
            log.info("Recebido evento CredencialCriada no Kafka para o documento: {} [tp2]", event.documento());

            // Garantir idempotência: verificar se pessoa já existe por documento
            if (pessoaRepository.existsByDocumento(event.documento())) {
                log.warn("Mensagem duplicada ou pessoa já existente com o documento {} [tp2]. Ignorando processamento.", event.documento());
                return;
            }

            // Garantir idempotência: verificar se pessoa já existe por credencialId
            if (event.credencialId() != null && pessoaRepository.existsByCredencialId(event.credencialId())) {
                log.warn("Mensagem duplicada ou pessoa já existente com a credencialId {} [tp2]. Ignorando processamento.", event.credencialId());
                return;
            }

            Pessoa novaPessoa = Pessoa.criar(
                    event.credencialId(),
                    event.nomeCompleto(),
                    event.documento(),
                    event.telefone(),
                    event.email()
            );

            pessoaRepository.save(novaPessoa);
            log.info("Pessoa criada assincronamente a partir de evento do IAM [tp2]. id={}", novaPessoa.getId());
        } finally {
            MDC.clear();
        }
    }
}
