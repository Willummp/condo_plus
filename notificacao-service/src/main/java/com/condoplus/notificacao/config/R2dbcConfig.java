package com.condoplus.notificacao.config;

import com.condoplus.notificacao.domain.Canal;
import com.condoplus.notificacao.domain.Notificacao;
import com.condoplus.notificacao.domain.PreferenciaNotificacao;
import com.condoplus.notificacao.domain.StatusNotificacao;
import com.condoplus.notificacao.domain.TipoEvento;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableR2dbcAuditing
@EnableR2dbcRepositories
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    private final ConnectionFactory connectionFactory;

    public R2dbcConfig(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public ConnectionFactory connectionFactory() {
        return this.connectionFactory;
    }

    @Bean
    BeforeConvertCallback<Notificacao> notificacaoIdGenerator() {
        return (notificacao, sqlIdentifier) -> {
            if (notificacao.getId() == null) {
                notificacao.setId(UUID.randomUUID());
            }
            return Mono.just(notificacao);
        };
    }

    @Bean
    BeforeConvertCallback<PreferenciaNotificacao> preferenciaIdGenerator() {
        return (preferencia, sqlIdentifier) -> {
            if (preferencia.getId() == null) {
                preferencia.setId(UUID.randomUUID());
            }
            return Mono.just(preferencia);
        };
    }

    @Override
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();

        converters.add(new CanalWritingConverter());
        converters.add(new StatusWritingConverter());
        converters.add(new TipoEventoWritingConverter());

        converters.add(new CanalReadingConverter());
        converters.add(new StatusReadingConverter());
        converters.add(new TipoEventoReadingConverter());

        return new R2dbcCustomConversions(getStoreConversions(), converters);
    }

    @WritingConverter
    private static class CanalWritingConverter implements Converter<Canal, String> {
        @Override
        public String convert(Canal source) {
            return source.name();
        }
    }

    @ReadingConverter
    private static class CanalReadingConverter implements Converter<String, Canal> {
        @Override
        public Canal convert(String source) {
            return Canal.valueOf(source);
        }
    }

    @WritingConverter
    private static class StatusWritingConverter implements Converter<StatusNotificacao, String> {
        @Override
        public String convert(StatusNotificacao source) {
            return source.name();
        }
    }

    @ReadingConverter
    private static class StatusReadingConverter implements Converter<String, StatusNotificacao> {
        @Override
        public StatusNotificacao convert(String source) {
            return StatusNotificacao.valueOf(source);
        }
    }

    @WritingConverter
    private static class TipoEventoWritingConverter implements Converter<TipoEvento, String> {
        @Override
        public String convert(TipoEvento source) {
            return source.name();
        }
    }

    @ReadingConverter
    private static class TipoEventoReadingConverter implements Converter<String, TipoEvento> {
        @Override
        public TipoEvento convert(String source) {
            return TipoEvento.valueOf(source);
        }
    }
}
