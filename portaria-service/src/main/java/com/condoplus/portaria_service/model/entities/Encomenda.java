package com.condoplus.portaria_service.model.entities;

import com.condoplus.portaria_service.model.enums.StatusEncomenda;
import com.condoplus.portaria_service.model.enums.TipoEncomenda;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "encomenda", schema = "portaria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Encomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEncomenda tipo;

    @Column(length = 500)
    private String descricao;

    @Column(name = "codigo_rastreio", length = 100)
    private String codigoRastreio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEncomenda status;

    @Column(name = "data_chegada", nullable = false, updatable = false)
    private LocalDateTime dataChegada;

    @Column(name = "data_retirada")
    private LocalDateTime dataRetirada;

    @Column(name = "porteiro_recebedor_id", nullable = false, updatable = false)
    private UUID porteiroRecebedorId;

    @Column(name = "porteiro_entregador_id")
    private UUID porteiroEntregadorId;

    @Column(name = "retirado_por_pessoa_id")
    private UUID retiradoPorPessoaId;

    @PrePersist
    void onCreate() {
        if (dataChegada == null) dataChegada = LocalDateTime.now();
        if (status == null) status = StatusEncomenda.AGUARDANDO_RETIRADA;

        validar();
    }

    public void validar() {
        if (unidadeId == null)
            throw new IllegalArgumentException("Unidade obrigatória");

        if (tipo == null)
            throw new IllegalArgumentException("Tipo obrigatório");

        if (porteiroRecebedorId == null)
            throw new IllegalArgumentException("Porteiro recebedor obrigatório");
    }

    public boolean estaExpirada(LocalDateTime agora) {
        return switch (tipo) {
            case CURTO_PRAZO -> agora.isAfter(dataChegada.plusHours(2));
            case MEDIO_PRAZO -> agora.isAfter(dataChegada.plusDays(7));
            case LONGO_PRAZO -> agora.isAfter(dataChegada.plusDays(30));
        };
    }

    public void marcarComoRetirada(UUID pessoaId, UUID porteiroId) {
        if (status == StatusEncomenda.RETIRADA)
            throw new IllegalStateException("Encomenda já retirada");

        if (status == StatusEncomenda.EXPIRADA)
            throw new IllegalStateException("Encomenda expirada");

        this.status = StatusEncomenda.RETIRADA;
        this.retiradoPorPessoaId = pessoaId;
        this.porteiroEntregadorId = porteiroId;
        this.dataRetirada = LocalDateTime.now();
    }

    public void marcarComoExpirada() {
        if (status == StatusEncomenda.RETIRADA)
            throw new IllegalStateException("Não pode expirar encomenda já retirada");

        this.status = StatusEncomenda.EXPIRADA;
    }
}