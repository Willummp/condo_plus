package com.condoplus.iam.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds
) {}
