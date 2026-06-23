package com.condoplus.auditoria.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Le a identidade que o API Gateway propaga e popula o contexto de seguranca.
 *
 * Arquitetura: a validacao do JWT (assinatura, expiracao) e responsabilidade do
 * Gateway. Apos validar, o Gateway injeta a identidade do usuario em headers
 * (X-User-Id, X-User-Roles) e encaminha a requisicao para este servico. O
 * auditoria nao revalida o token; ele confia no Gateway, mas exige a prova de
 * que a requisicao passou por ele -- a presenca do X-User-Id.
 *
 * Sem o header, o contexto fica vazio e o Spring Security rejeita a requisicao
 * (403) na regra authenticated() do SecurityConfig. Assim uma requisicao que
 * nao passou pelo Gateway nao acessa as rotas protegidas.
 *
 * Os papeis vem em X-User-Roles separados por virgula (ex.: "SINDICO,MORADOR").
 * Cada papel vira uma authority com prefixo ROLE_, que e a convencao que o
 * Spring espera ao avaliar hasRole("SINDICO") no SecurityConfig.
 */
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLES = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(HEADER_USER_ID);

        // Sem identidade propagada pelo Gateway: nao autentica. O contexto fica
        // vazio e a requisicao sera barrada pela regra authenticated() adiante.
        if (StringUtils.hasText(userId)) {
            List<SimpleGrantedAuthority> authorities = parseRoles(request.getHeader(HEADER_USER_ROLES));

            // O principal e o userId; sem credentials porque o token ja foi
            // validado no Gateway -- aqui so confiamos na identidade propagada.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Converte "SINDICO,MORADOR" em authorities [ROLE_SINDICO, ROLE_MORADOR].
     * Tolera espacos e header ausente/vazio (nesse caso, usuario autenticado
     * sem papel -- acessa o que exige apenas autenticacao, mas nao o que exige
     * um papel especifico).
     */
    private List<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        if (!StringUtils.hasText(rolesHeader)) {
            return List.of();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}