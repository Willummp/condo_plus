package com.condoplus.condominio.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Configuração de infraestrutura do Kafka (tp2).
 * 
 * <p>Define o tratamento global de erros para os listeners do Kafka.
 * Caso ocorra um erro irrecuperável no processamento de mensagens (como duplicidade persistente,
 * erros de desserialização ou validação de regras de negócio), a mensagem é reenviada para uma
 * Dead Letter Queue (DLQ/DLT) após um número definido de tentativas (Backoff).
 */
@Configuration
@Slf4j
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        log.info("Inicializando DefaultErrorHandler com DeadLetterPublishingRecoverer (DLT) [tp2]");
        
        // DeadLetterPublishingRecoverer publica na fila de erro (nome_do_topico.DLT)
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);
        
        // Define 3 tentativas (1 original + 2 retentativas) com intervalo de 2 segundos entre elas
        return new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 2));
    }
}
