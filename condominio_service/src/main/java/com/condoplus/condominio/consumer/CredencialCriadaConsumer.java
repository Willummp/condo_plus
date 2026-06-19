package com.condoplus.condominio.consumer;

import com.condoplus.condominio.event.CredencialCriadaEvent;
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
        if (event.correlationId() != null) {
            MDC.put("correlationId", event.correlationId());
        }
        try {
            log.info("Recebido evento CredencialCriada [tp2]. credencialId={} email={}",
                    event.credencialId(), event.email());

            if (event.credencialId() != null && pessoaRepository.existsByCredencialId(event.credencialId())) {
                log.info("Credencial {} já vinculada a uma pessoa no condomínio. Ignorando [tp2].",
                        event.credencialId());
                return;
            }

            log.warn("Credencial {} criada no IAM sem pessoa correspondente no condomínio [tp2]. " +
                    "O cadastro da pessoa deve ser realizado via POST /pessoas.", event.credencialId());
        } finally {
            MDC.clear();
        }
    }
}
