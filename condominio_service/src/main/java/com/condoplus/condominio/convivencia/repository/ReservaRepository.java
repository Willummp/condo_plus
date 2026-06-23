package com.condoplus.condominio.convivencia.repository;

import com.condoplus.condominio.convivencia.domain.Reserva;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositório de persistência para a entidade Reserva (Aggregate Root).
 * 
 * <p>Anotações e conceitos importantes:
 * <ul>
 *   <li>{@code CrudRepository<Reserva, UUID>} — Interface base do Spring Data JDBC que fornece as operações padrão de CRUD para a entidade Reserva.</li>
 * </ul>
 */
public interface ReservaRepository extends CrudRepository<Reserva, UUID> {

    /**
     * Recupera todas as reservas registradas para um determinado morador de forma cronológica reversa.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Query} — Executa uma consulta SQL nativa personalizada mapeada pelo Spring Data JDBC.</li>
     * </ul>
     * 
     * @param moradorId ID único (UUID) do morador.
     * @return Lista de reservas encontradas.
     */
    @Query("SELECT * FROM condominio.reserva WHERE morador_id = :moradorId ORDER BY data_reserva DESC")
    List<Reserva> findByMorador(UUID moradorId);

    /**
     * Consulta as reservas registradas para uma área comum específica em uma determinada data de interesse.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Query} — Executa uma consulta SQL nativa para filtragem exata de data.</li>
     * </ul>
     * 
     * @param areaComumId ID da área comum.
     * @param data Data do agendamento.
     * @return Lista de reservas encontradas e ordenadas por horário de início.
     */
    @Query("SELECT * FROM condominio.reserva WHERE area_comum_id = :areaComumId AND data_reserva = :data ORDER BY hora_inicio")
    List<Reserva> findByAreaComumEData(UUID areaComumId, LocalDate data);

    /**
     * Detecta agendamentos com status 'CONFIRMADA' que apresentem sobreposição de horário no período solicitado.
     * 
     * <p><b>Lógica de Detecção de Conflitos:</b>
     * Dois intervalos de tempo [a, b] e [c, d] apresentam sobreposição se e somente se: 
     * {@code hora_inicio_existente < hora_fim_nova} E {@code hora_fim_existente > hora_inicio_nova}.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Query} — Executa a query de validação atômica de concorrência.</li>
     * </ul>
     * 
     * @param areaComumId ID da área comum avaliada.
     * @param data Data que se deseja verificar.
     * @param horaInicio Horário de início do novo agendamento.
     * @param horaFim Horário de término do novo agendamento.
     * @return Lista de reservas conflitantes (se vazia, o horário está livre).
     */
    @Query("""
        SELECT * FROM condominio.reserva
        WHERE area_comum_id = :areaComumId
          AND data_reserva  = :data
          AND status        = 'CONFIRMADA'
          AND hora_inicio   < :horaFim
          AND hora_fim      > :horaInicio
        """)
    List<Reserva> findConflitos(UUID areaComumId, LocalDate data,
                                LocalTime horaInicio, LocalTime horaFim);
}
