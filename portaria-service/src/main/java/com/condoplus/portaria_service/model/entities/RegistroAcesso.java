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
    @Column(name = "tipo_pessoa", nullable = false, length = 20)
    private TipoPessoaAcesso tipoPessoa;

    /**
     * ID da pessoa que entrou/saiu.
     * Para MORADOR/FUNCIONARIO/PRESTADOR: aponta para Pessoa do condominio-service.
     * Para VISITANTE: aponta para Visitante deste serviço.
     * Um único campo — não há visitante_id separado.
     */
    @Column(name = "pessoa_id", nullable = false)
    private UUID pessoaId;

    @Column(name = "unidade_id")
    private UUID unidadeId;

    @Column(name = "veiculo_placa", length = 10)
    private String veiculoPlaca;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento", nullable = false, length = 10)
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

        if (pessoaId == null)
            throw new IllegalArgumentException("pessoa_id obrigatório");
    }
}