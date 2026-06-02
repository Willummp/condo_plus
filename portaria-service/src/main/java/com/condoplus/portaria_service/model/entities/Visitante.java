package com.condoplus.portaria_service.model.entities;

import com.condoplus.portaria_service.model.enums.StatusVisitante;
import com.condoplus.portaria_service.model.enums.TipoVisitante;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "visitante", schema = "portaria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visitante {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 200)
    private String nome;
    @Column(length = 20)
    private String documento;
    @Column(length = 20)
    private String telefone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoVisitante tipo;
    @Column(name = "autorizado_por_pessoa_id", nullable = false)
    private UUID autorizadoPorPessoaId; // Pessoa do condominio-service
    @Column(name = "autorizado_para_unidade_id", nullable = false)
    private UUID autorizadoParaUnidadeId; // Unidade do condominio-service
    @Column(name = "validade_inicio", nullable = false)
    private LocalDateTime validadeInicio;
    @Column(name = "validade_fim", nullable = false)
    private LocalDateTime validadeFim;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusVisitante status;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
    @PrePersist
    void onCreate() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusVisitante.AUTORIZADO;
        }
    }
    public boolean estaAtivo(LocalDateTime referencia) {
        return status == StatusVisitante.AUTORIZADO
                && !referencia.isBefore(validadeInicio)
                && !referencia.isAfter(validadeFim);
    }
}