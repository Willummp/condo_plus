package com.condoplus.auditoria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracao de seguranca do auditoria-service.
 *
 * Arquitetura: a validacao do JWT vive no API Gateway. Apos validar, o Gateway
 * propaga a identidade do usuario em headers (X-User-Id, X-User-Roles). O
 * GatewayAuthenticationFilter le esses headers e popula o contexto de seguranca;
 * este config define o que cada rota exige.
 *
 * Proteja-se por ator, nao por uniformidade:
 * - rotas de auditoria (gravar/consultar) sao trafego servico-a-servico:
 *   exigem apenas que a requisicao tenha passado pelo Gateway (autenticada).
 * - triar uma anomalia (PATCH de status) e uma acao humana sensivel -- silenciar
 *   um alerta de seguranca -- e exige o papel de sindico/admin.
 *
 * stateless: sem sessao; cada request e independente, coerente com microsservico
 * atras de Gateway.
 */
@Configuration
public class SecurityConfig {

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    public SecurityConfig(GatewayAuthenticationFilter gatewayAuthenticationFilter) {
        this.gatewayAuthenticationFilter = gatewayAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF nao se aplica a API REST stateless consumida por outros servicos
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints de monitoramento expostos de proposito (nao /actuator/**:
                        // endpoints sensiveis como env/heapdump seguem protegidos).
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/metrics",
                                "/actuator/prometheus",
                                // Paths que chegam via gateway com StripPrefix=1 (/api/auditoria/** → /auditoria/**)
                                "/auditoria/actuator/health",
                                "/auditoria/actuator/info",
                                "/auditoria/actuator/metrics",
                                "/auditoria/actuator/prometheus"
                        ).permitAll()

                        // REGRA ESPECIFICA PRIMEIRO: triar anomalia (mudar status) e acao de
                        // sindico/admin. Precisa vir antes da regra generica /auditoria/**,
                        // senao aquela capturaria este PATCH antes desta ser avaliada.
                        // hasAnyRole("SINDICO","ADMIN") casa com as authorities ROLE_SINDICO /
                        // ROLE_ADMIN que o filtro grava a partir do header X-User-Roles.
                        .requestMatchers(HttpMethod.PATCH, "/auditoria/anomalias/*/status")
                        .hasAnyRole("SINDICO", "ADMIN")

                        // REGRA GERAL: demais rotas de auditoria exigem apenas autenticacao
                        // (ter passado pelo Gateway). Sem o X-User-Id, o filtro nao autentica
                        // e o Spring rejeita aqui com 403.
                        .requestMatchers("/auditoria/**").authenticated()

                        // qualquer outra coisa exige autenticacao
                        .anyRequest().authenticated()
                )
                // Coloca o filtro do Gateway ANTES da etapa de autenticacao padrao, para
                // que o contexto ja esteja populado quando a autorizacao for avaliada.
                .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}