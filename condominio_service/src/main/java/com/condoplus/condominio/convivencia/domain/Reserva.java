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

    
    public void cancelar() {
        this.status = StatusReserva.CANCELADA;
    }
}
