package com.condoplus.condominio.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        log.info("Inicializando DefaultErrorHandler com DeadLetterPublishingRecoverer (DLT) [tp2]");
        

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);
        

        return new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 2));
    }
}
