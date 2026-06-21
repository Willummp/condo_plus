package com.condoplus.portaria_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // Habilita @PreAuthorize nos controllers — sem isso é decoração
public class SecurityConfig {

    /**
     * Registra o HeaderAuthenticationFilter antes do filtro padrão do Spring Security.
     * Ordem importa: o header precisa ser lido antes de qualquer checagem de autenticação.
     */
    @Bean
    public HeaderAuthenticationFilter headerAuthenticationFilter() {
        return new HeaderAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // API REST stateless — sem sessão nem CSRF
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Injeta o filtro de header antes do filtro de autenticação padrão
                .addFilterBefore(
                        headerAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        // Actuator aberto para health checks da infra
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Tudo o mais exige autenticação;
                        // as regras finas (ROLE_PORTEIRO, ROLE_SINDICO) ficam nos @PreAuthorize
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}