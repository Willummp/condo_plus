package com.condoplus.condominio.consumer;

import com.condoplus.condominio.estrutura.domain.Pessoa;
import com.condoplus.condominio.estrutura.repository.PessoaRepository;
import com.condoplus.condominio.event.CredencialCriadaEvent;
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
        if (event.correlationId() != null) {
            MDC.put("correlationId", event.correlationId());
        }
        try {
            log.info("Recebido evento CredencialCriada [tp2]. credencialId={} email={}",
                    event.credencialId(), event.email());

            if (event.credencialId() != null && pessoaRepository.existsByCredencialId(event.credencialId())) {
                log.info("Credencial {} já vinculada a uma Pessoa — ignorando (idempotência) [tp2].",
                        event.credencialId());
                return;
            }

            if (event.documento() == null || event.nomeCompleto() == null) {
                log.warn("Credencial {} criada no IAM sem dados de pessoa no evento [tp2]. " +
                        "Cadastro deve ser feito via POST /pessoas.", event.credencialId());
                return;
            }

            if (pessoaRepository.existsByDocumento(event.documento())) {
                log.info("Pessoa com documento {} já existe — vinculando credencial [tp2].", event.documento());
                Pessoa existente = pessoaRepository.findByDocumento(event.documento()).orElseThrow();
                existente.setCredencialId(event.credencialId());
                pessoaRepository.save(existente);
                return;
            }

            Pessoa pessoa = Pessoa.criar(
                    event.credencialId(),
                    event.nomeCompleto(),
                    event.documento(),
                    event.telefone(),
                    event.email()
            );
            pessoaRepository.save(pessoa);
            log.info("Pessoa criada a partir do evento CredencialCriada [tp2]. credencialId={} documento={}",
                    event.credencialId(), event.documento());
        } finally {
            MDC.clear();
        }
    }
}
