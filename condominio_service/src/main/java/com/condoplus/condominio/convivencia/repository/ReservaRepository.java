package com.condoplus.condominio.convivencia.repository;

import com.condoplus.condominio.convivencia.domain.Reserva;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ReservaRepository extends CrudRepository<Reserva, UUID> {

    
    @Query("SELECT * FROM condominio.reserva WHERE morador_id = :moradorId ORDER BY data_reserva DESC")
    List<Reserva> findByMorador(UUID moradorId);

    
    @Query("SELECT * FROM condominio.reserva WHERE area_comum_id = :areaComumId AND data_reserva = :data ORDER BY hora_inicio")
    List<Reserva> findByAreaComumEData(UUID areaComumId, LocalDate data);

    
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
