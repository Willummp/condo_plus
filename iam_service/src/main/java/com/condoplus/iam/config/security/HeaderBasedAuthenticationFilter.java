package com.condoplus.iam.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class HeaderBasedAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_USER_ROLES = "X-User-Roles";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String userEmail = request.getHeader(HEADER_USER_EMAIL);
        String userRoles = request.getHeader(HEADER_USER_ROLES);

        if (userId != null && userRoles != null) {

            List<SimpleGrantedAuthority> authorities =
                    Arrays.stream(userRoles.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

            if (userEmail != null) {
                auth.setDetails(userEmail);
            }

            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug(
                    "Autenticação populada a partir de headers do Gateway. userId={} roles={}",
                    userId,
                    userRoles
            );
        }

        filterChain.doFilter(request, response);
    }
}