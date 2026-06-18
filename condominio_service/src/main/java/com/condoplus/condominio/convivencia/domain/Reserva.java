package com.condoplus.condominio.convivencia.domain;

import com.condoplus.condominio.estrutura.domain.Pessoa;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Solicitação de uso exclusivo de uma área comum do condomínio.
 * 
 * <p><b>Invariante Crítica:</b>
 * Não podem existir duas reservas com status CONFIRMADA para a mesma área, na mesma data,
 * com qualquer sobreposição de horário. A segurança concorrência desta regra é gerenciada 
 * por transação {@code SERIALIZABLE} no {@code ReservaService}.
 * 
 * <p>Anotações da classe:
 * <ul>
 *   <li>{@code @Table(schema = "condominio", value = "reserva")} — Mapeia a classe para a tabela {@code reserva} do schema {@code condominio}.</li>
 *   <li>{@code @Getter} — Gera os métodos getters pelo Lombok.</li>
 *   <li>{@code @Setter} — Gera os métodos setters pelo Lombok.</li>
 *   <li>{@code @NoArgsConstructor} — Gera o construtor padrão vazio pelo Lombok.</li>
 * </ul>
 */
@Table(schema = "condominio", value = "reserva")
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

    @Id
    private UUID id;

    @Column("area_comum_id")
    private AggregateReference<AreaComum, UUID> areaComumId;

    @Column("morador_id")
    private AggregateReference<Pessoa, UUID> moradorId;

    @Column("data_reserva")
    private LocalDate dataReserva;

    @Column("hora_inicio")
    private LocalTime horaInicio;

    @Column("hora_fim")
    private LocalTime horaFim;

    @Column("status")
    private StatusReserva status;

    @CreatedDate
    @Column("criada_em")
    private LocalDateTime criadaEm;

    /**
     * Cancela a reserva ativa, alterando o status interno da entidade.
     */
    public void cancelar() {
        this.status = StatusReserva.CANCELADA;
    }
}
