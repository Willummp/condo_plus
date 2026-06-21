package com.condoplus.portaria_service.model.enums;

public enum TipoEncomenda {
    CURTO_PRAZO, // iFood — 2h Redis TTL
    MEDIO_PRAZO, // Amazon — 7 dias
    LONGO_PRAZO // Shopee — 30 dias
}
