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
    @Column(nullable = false, length = 20)
    private TipoEncomenda tipo;
    @Column(length = 500)
    private String descricao; // opcional, ex: "caixa pequena Amazon"
    @Column(name = "codigo_rastreio", length = 100)
    private String codigoRastreio; // opcional
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusEncomenda status;
    @Column(name = "data_chegada", nullable = false, updatable = false)
    private LocalDateTime dataChegada;
    @Column(name = "data_retirada")
    private LocalDateTime dataRetirada;
    @Column(name = "porteiro_recebedor_id", nullable = false, updatable = false)
    private UUID porteiroRecebedorId; // Pessoa estável (princípio do 1.6)
    @Column(name = "porteiro_entregador_id")
    private UUID porteiroEntregadorId; // Pessoa estável
    @Column(name = "retirado_por_pessoa_id")
    private UUID retiradoPorPessoaId; // Pessoa estável
    @PrePersist
    void onCreate() {
        if (dataChegada == null) {
            dataChegada = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusEncomenda.AGUARDANDO_RETIRADA;
        }
    }
}