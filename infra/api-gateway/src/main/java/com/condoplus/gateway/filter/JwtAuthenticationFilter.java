package com.condoplus.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtro de Gateway para validação de JWT e propagação de identidade.
 * 
 * <p>Intercepta requisições destinadas a rotas protegidas, valida a assinatura e expiração do
 * token JWT recebido no cabeçalho Authorization, e propaga os dados do usuário (ID, Email e Roles)
 * via headers HTTP seguros para os microsserviços downstream.
 */
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret:CondoPlusSuperSecretKeyForJWTAuth2026!}")
    private String jwtSecret;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Verificar a presença do header Authorization
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Tentativa de acesso a rota protegida sem cabeçalho Authorization");
                return onError(exchange, "Cabeçalho Authorization ausente", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Cabeçalho Authorization com formato inválido");
                return onError(exchange, "Formato do token inválido. Use 'Bearer <token>'", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                // 2. Validar assinatura e expiração do token
                Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT jwt = verifier.verify(token);

                // 3. Extrair os claims relevantes
                String userId = jwt.getSubject();
                String email = jwt.getClaim("email").asString();
                
                // Trata as roles que podem vir como lista ou string do iam-service
                Claim rolesClaim = jwt.getClaim("roles");
                String rolesStr = "";
                if (!rolesClaim.isMissing() && !rolesClaim.isNull()) {
                    try {
                        List<String> rolesList = rolesClaim.asList(String.class);
                        if (rolesList != null) {
                            rolesStr = rolesList.stream().collect(Collectors.joining(","));
                        }
                    } catch (Exception e) {
                        rolesStr = rolesClaim.asString();
                    }
                }

                // Fallback para claim singular "role" se "roles" estiver ausente
                if (rolesStr == null || rolesStr.isBlank()) {
                    Claim roleClaim = jwt.getClaim("role");
                    if (!roleClaim.isMissing() && !roleClaim.isNull()) {
                        rolesStr = roleClaim.asString();
                    }
                }

                log.debug("JWT validado com sucesso. userId={}, email={}, roles={}", userId, email, rolesStr);

                // 4. Mudar a requisição para adicionar os cabeçalhos de contexto seguros
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Email", email != null ? email : "")
                        .header("X-User-Roles", rolesStr != null ? rolesStr : "")
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (JWTVerificationException ex) {
                log.error("Falha na validação do token JWT: {}", ex.getMessage());
                return onError(exchange, "Token inválido ou expirado: " + ex.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("X-Error-Reason", err);
        return response.setComplete();
    }

    public static class Config {
        // Configurações adicionais se necessário
    }
}
