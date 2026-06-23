package com.condoplus.portaria_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Lê os headers X-User-Id e X-User-Roles propagados pelo API Gateway
 * e popula o SecurityContext para que @PreAuthorize e @AuthenticationPrincipal
 * funcionem em todos os controllers.
 *
 * Fluxo:
 *   Gateway autentica o JWT → extrai userId e roles → injeta como headers
 *   Este filtro lê os headers → monta Authentication → coloca no contexto
 *
 * X-User-Id:    UUID da Pessoa (usado como @AuthenticationPrincipal)
 * X-User-Roles: roles separadas por vírgula, ex: "ROLE_PORTEIRO,ROLE_MORADOR"
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID    = "X-User-Id";
    private static final String HEADER_USER_ROLES = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String rolesHeader = request.getHeader(HEADER_USER_ROLES);

        if (userId != null && !userId.isBlank()) {
            List<SimpleGrantedAuthority> authorities = parseAuthorities(rolesHeader);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseAuthorities(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(r -> !r.isBlank())
                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .toList();
    }
}