package com.condoplus.iam.service;

import com.condoplus.iam.config.properties.JwtProperties;
import com.condoplus.iam.config.properties.SecurityProperties;
import com.condoplus.iam.domain.CredencialUsuario;
import com.condoplus.iam.domain.StatusCredencial;
import com.condoplus.iam.dto.LoginRequest;
import com.condoplus.iam.dto.TokenResponse;
import com.condoplus.iam.exception.CredencialBloqueadaException;
import com.condoplus.iam.exception.CredenciaisInvalidasException;
import com.condoplus.iam.repository.CredencialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutenticacaoService {

    private final CredencialRepository credencialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final SecurityProperties securityProperties;

    @Transactional
    public TokenResponse autenticar(LoginRequest req) {

        Optional<CredencialUsuario> opt =
                credencialRepository.findByEmail(req.email());

        if (opt.isEmpty()) {
            log.warn("Login falhado: e-mail não cadastrado.");
            throw new CredenciaisInvalidasException();
        }

        CredencialUsuario credencial = opt.get();

        verificarBloqueio(credencial);

        if (!passwordEncoder.matches(
                req.senha(),
                credencial.getSenhaHash()
        )) {
            tratarFalha(credencial);
            throw new CredenciaisInvalidasException();
        }

        credencial.setTentativasFalhas(0);
        credencial.setBloqueadoAte(null);
        credencial.setUltimoLogin(LocalDateTime.now());

        credencialRepository.save(credencial);

        log.info(
                "Login realizado com sucesso. credencialId={}",
                credencial.getId()
        );

        String accessToken = jwtService.gerarToken(credencial);
        String refreshToken = jwtService.gerarRefreshToken(credencial);

        return new TokenResponse(accessToken, refreshToken, jwtProperties.expirationSeconds());
    }

    @Transactional
    public TokenResponse renovarToken(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.extrairClaims(refreshToken);
        } catch (Exception e) {
            log.warn("Refresh token inválido ou expirado");
            throw new CredenciaisInvalidasException();
        }

        String tokenType = claims.get("tokenType", String.class);
        if (!"refresh".equals(tokenType)) {
            log.warn("Token fornecido não é do tipo refresh");
            throw new CredenciaisInvalidasException();
        }

        UUID credencialId;
        try {
            credencialId = UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            throw new CredenciaisInvalidasException();
        }

        CredencialUsuario credencial = credencialRepository.findById(credencialId)
                .orElseThrow(() -> {
                    log.warn("Credencial não encontrada durante refresh. id={}", credencialId);
                    return new CredenciaisInvalidasException();
                });

        verificarBloqueio(credencial);

        String novoAccessToken = jwtService.gerarToken(credencial);
        String novoRefreshToken = jwtService.gerarRefreshToken(credencial);

        log.info("Tokens renovados. credencialId={}", credencialId);

        return new TokenResponse(novoAccessToken, novoRefreshToken, jwtProperties.expirationSeconds());
    }

    private void verificarBloqueio(CredencialUsuario credencial) {

        if (credencial.getStatus() == StatusCredencial.BLOQUEADO) {

            log.warn(
                    "Login negado: credencial bloqueada manualmente. credencialId={}",
                    credencial.getId()
            );

            throw new CredencialBloqueadaException();
        }

        if (credencial.getStatus()
                == StatusCredencial.BLOQUEADO_TEMPORARIAMENTE) {

            if (credencial.getBloqueadoAte() != null
                    && LocalDateTime.now()
                    .isBefore(credencial.getBloqueadoAte())) {

                log.warn(
                        "Login negado: credencial em bloqueio temporário. credencialId={}",
                        credencial.getId()
                );

                throw new CredencialBloqueadaException();
            }

            log.info(
                    "Bloqueio temporário expirou. Desbloqueando credencial id={}",
                    credencial.getId()
            );

            credencial.setStatus(StatusCredencial.ATIVO);
            credencial.setTentativasFalhas(0);
            credencial.setBloqueadoAte(null);
        }
    }

    private void tratarFalha(CredencialUsuario credencial) {

        int tentativas = credencial.getTentativasFalhas() + 1;

        credencial.setTentativasFalhas(tentativas);

        if (tentativas >= securityProperties.maxFailedAttempts()) {

            credencial.setStatus(
                    StatusCredencial.BLOQUEADO_TEMPORARIAMENTE
            );

            credencial.setBloqueadoAte(
                    LocalDateTime.now().plusMinutes(
                            securityProperties.lockoutDurationMinutes()
                    )
            );

            log.warn(
                    "Credencial bloqueada por excesso de tentativas. credencialId={} tentativas={}",
                    credencial.getId(),
                    tentativas
            );

        } else {

            log.warn(
                    "Senha incorreta. credencialId={} tentativas={}",
                    credencial.getId(),
                    tentativas
            );
        }

        credencialRepository.save(credencial);
    }
}