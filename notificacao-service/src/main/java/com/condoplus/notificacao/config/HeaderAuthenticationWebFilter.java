package com.condoplus.notificacao.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HeaderAuthenticationWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userId    = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String rolesHdr  = exchange.getRequest().getHeaders().getFirst("X-User-Roles");

        if (userId != null && rolesHdr != null && !rolesHdr.isBlank()) {
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHdr.split(","))
                    .map(String::trim)
                    .filter(r -> !r.isBlank())
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());

            Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
        }

        return chain.filter(exchange);
    }
}
