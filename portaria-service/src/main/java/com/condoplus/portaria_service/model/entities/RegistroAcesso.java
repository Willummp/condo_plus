package com.condoplus.portaria_service.model.entities;

import com.condoplus.portaria_service.model.enums.TipoMovimento;
import com.condoplus.portaria_service.model.enums.TipoPessoaAcesso;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "registro_acesso", schema = "portaria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false)
    private TipoPessoaAcesso tipoPessoa;

    @Column(name = "pessoa_id")
    private UUID pessoaId;

    @Column(name = "visitante_id")
    private UUID visitanteId;

    @Column(name = "unidade_id")
    private UUID unidadeId;

    @Column(name = "veiculo_placa", length = 10)
    private String veiculoPlaca;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento", nullable = false)
    private TipoMovimento tipoMovimento;

    @Column(name = "timestamp_acesso", nullable = false)
    private LocalDateTime timestampAcesso;

    @Column(name = "porteiro_id", nullable = false)
    private UUID porteiroId;

    @Column(length = 500)
    private String observacoes;

    @PrePersist
    void onCreate() {
        if (timestampAcesso == null) {
            timestampAcesso = LocalDateTime.now();
        }
        validar();
    }

    public void validar() {
        if (tipoPessoa == null)
            throw new IllegalArgumentException("Tipo de pessoa obrigatório");

        if (tipoMovimento == null)
            throw new IllegalArgumentException("Tipo de movimento obrigatório");

        if (porteiroId == null)
            throw new IllegalArgumentException("Porteiro obrigatório");

        if (tipoPessoa == TipoPessoaAcesso.VISITANTE) {
            if (visitanteId == null || pessoaId != null)
                throw new IllegalArgumentException("Visitante deve ter visitanteId apenas");
        } else {
            if (pessoaId == null || visitanteId != null)
                throw new IllegalArgumentException("Pessoa deve ter pessoaId apenas");
        }
    }
}