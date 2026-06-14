package com.condoplus.iam.domain;

public enum StatusCredencial {
    ATIVO,
    BLOQUEADO, // bloqueio manual pelo ADMIN
    BLOQUEADO_TEMPORARIAMENTE // bloqueio automático por tentativas falhas
}
