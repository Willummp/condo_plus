package com.condoplus.iam.controller;

import com.condoplus.iam.dto.LoginRequest;
import com.condoplus.iam.dto.TokenResponse;
import com.condoplus.iam.service.AutenticacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest req) {

        TokenResponse response =
                autenticacaoService.autenticar(req);

        return ResponseEntity.ok(response);
    }
/*
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestHeader("Authorization")
            String authorizationHeader) {

        String tokenExpirado =
                authorizationHeader.startsWith("Bearer ")
                        ? authorizationHeader.substring(7)
                        : authorizationHeader;

        TokenResponse response =
                autenticacaoService.renovarToken(tokenExpirado);

        return ResponseEntity.ok(response);
    }

 */
}

