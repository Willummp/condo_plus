package com.condoplus.auditoria.messaging;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

/**
 * Configuracao do consumo Kafka do auditoria-service.
 *
 * Dois pontos centrais:
 *
 * 1) DESSERIALIZACAO (Opcao B): o produtor (condominio-service) grava o tipo
 *    da classe dele no header da mensagem. Se deixassemos o default, o
 *    JsonDeserializer tentaria instanciar com.condoplus.condominio.event.
 *    EventEnvelope — classe que NAO existe aqui. Por isso configuramos o
 *    deserializer para IGNORAR o header de tipo (setUseTypeHeaders=false) e
 *    desserializar sempre para o NOSSO EventEnvelope. Isso concretiza o
 *    desacoplamento: lemos o evento no nosso formato, sem depender do deles.
 *
 * 2) RESILIENCIA (padrao do grupo): ErrorHandlingDeserializer envolve o
 *    JsonDeserializer (mensagem malformada nao derruba o consumer) e o
 *    DefaultErrorHandler reenvia para um Dead Letter Topic (<topico>.DLT)
 *    apos 3 tentativas com intervalo de 2s.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    /**
     * ConsumerFactory que desserializa o value para o nosso EventEnvelope,
     * ignorando o type header do produtor.
     */
    @Bean
    public ConsumerFactory<String, EventEnvelope> consumerFactory(KafkaProperties props) {
        Map<String, Object> config = props.buildConsumerProperties(null);

        // Desserializador do nosso EventEnvelope, ignorando o header de tipo do produtor.
        JsonDeserializer<EventEnvelope> jsonDeserializer = new JsonDeserializer<>(EventEnvelope.class);
        jsonDeserializer.setUseTypeHeaders(false); // <-- chave da Opcao B
        jsonDeserializer.addTrustedPackages("*");

        // Envolve o JsonDeserializer com tratamento de erro (poison message -> DLT).
        ErrorHandlingDeserializer<EventEnvelope> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),       // key
                errorHandlingDeserializer       // value
        );
    }

    /**
     * Factory dos @KafkaListener, com o error handler + DLT acoplado.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventEnvelope> kafkaListenerContainerFactory(
            ConsumerFactory<String, EventEnvelope> consumerFactory,
            KafkaTemplate<Object, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, EventEnvelope> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Apos 3 tentativas (1 + 2 retentativas, 2s de intervalo), envia para <topico>.DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 2)));

        return factory;
    }
}