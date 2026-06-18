package com.condoplus.auditoria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracao de seguranca do auditoria-service (TP1).
 *
 * Decisao consciente: no TP1 a borda e simples. A autenticacao forte
 * (validacao de JWT) vive no API Gateway, que e quem protege as rotas
 * antes de chegar aqui. Este servico, internamente, libera os endpoints
 * de auditoria e o health-check do Actuator.
 *
 * Sem este bean, o Spring Boot aplica a config padrao (HTTP Basic com
 * senha aleatoria gerada a cada boot) — inadequada para entrega e demo.
 *
 * stateless: o servico nao guarda sessao; cada request e independente,
 * coerente com microsservico atras de Gateway.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF nao se aplica a API REST stateless consumida por outros servicos
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // endpoints de monitoramento expostos no application.yml.
                        // Liberados de forma cirurgica (nao /actuator/**): so os que
                        // expostos de proposito. Endpoints sensiveis (env, heapdump)
                        // permaneceriam protegidos pelo anyRequest().authenticated().
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/metrics",
                                "/actuator/prometheus"
                        ).permitAll()
                        // endpoints de auditoria liberados internamente (Gateway protege a borda)
                        .requestMatchers("/auditoria/**").permitAll()
                        // qualquer outra coisa exige autenticacao
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}