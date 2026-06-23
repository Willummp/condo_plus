package com.condoplus.condominio.convivencia.service;

import com.condoplus.condominio.convivencia.domain.Reserva;
import com.condoplus.condominio.convivencia.domain.StatusReserva;
import com.condoplus.condominio.convivencia.dto.NovaReservaRequest;
import com.condoplus.condominio.convivencia.dto.ReservaResponse;
import com.condoplus.condominio.convivencia.repository.AreaComumRepository;
import com.condoplus.condominio.convivencia.repository.ReservaRepository;
import com.condoplus.condominio.estrutura.repository.PessoaRepository;
import com.condoplus.condominio.exception.AreaComumIndisponivelException;
import com.condoplus.condominio.exception.AreaComumNaoEncontradaException;
import com.condoplus.condominio.exception.ConflitoReservaException;
import com.condoplus.condominio.exception.PessoaNaoEncontradaException;
import com.condoplus.condominio.event.ReservaConfirmadaEvent;
import com.condoplus.condominio.producer.CondominioEventProducer;
import org.slf4j.MDC;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final AreaComumRepository areaComumRepository;
    private final PessoaRepository pessoaRepository;
    private final CondominioEventProducer eventProducer;
    private final Counter reservasCounter;

    public ReservaService(ReservaRepository reservaRepository,
                          AreaComumRepository areaComumRepository,
                          PessoaRepository pessoaRepository,
                          CondominioEventProducer eventProducer,
                          MeterRegistry meterRegistry) {
        this.reservaRepository = reservaRepository;
        this.areaComumRepository = areaComumRepository;
        this.pessoaRepository = pessoaRepository;
        this.eventProducer = eventProducer;
        this.reservasCounter = Counter.builder("condoplus.reservas.confirmadas")
                .description("Quantidade total de reservas confirmadas no condominio")
                .register(meterRegistry);
    }

    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservaResponse criar(NovaReservaRequest req, UUID moradorId) {
        var area = areaComumRepository.findById(req.areaComumId())
            .orElseThrow(() -> new AreaComumNaoEncontradaException(req.areaComumId()));

        if (!area.isAtiva()) {
            throw new AreaComumIndisponivelException(area.getNome());
        }

        List<Reserva> conflitos = reservaRepository.findConflitos(
            req.areaComumId(), req.dataReserva(), req.horaInicio(), req.horaFim());

        if (!conflitos.isEmpty()) {
            log.warn("Conflito de reserva detectado. areaComumId={} data={} {}-{}",
                req.areaComumId(), req.dataReserva(), req.horaInicio(), req.horaFim());
            throw new ConflitoReservaException(area.getNome(), req.dataReserva());
        }

        UUID pessoaId = pessoaRepository.findByCredencialId(moradorId)
                .orElseThrow(() -> new PessoaNaoEncontradaException(moradorId))
                .getId();

        Reserva r = new Reserva();
        r.setAreaComumId(AggregateReference.to(req.areaComumId()));
        r.setMoradorId(AggregateReference.to(pessoaId));
        r.setDataReserva(req.dataReserva());
        r.setHoraInicio(req.horaInicio());
        r.setHoraFim(req.horaFim());
        r.setStatus(StatusReserva.CONFIRMADA);

        Reserva salva = reservaRepository.save(r);
        this.reservasCounter.increment();

        eventProducer.publicarReserva(new ReservaConfirmadaEvent(
                salva.getId(),
                req.areaComumId(),
                moradorId,
                req.dataReserva(),
                req.horaInicio(),
                req.horaFim(),
                MDC.get("correlationId")
        ));

        log.info("Reserva confirmada. id={} area={} data={}", salva.getId(), area.getNome(), salva.getDataReserva());
        return ReservaResponse.fromEntity(salva, area.getNome());
    }

    
    @Transactional
    public void cancelar(UUID reservaId, UUID moradorId) {
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new RuntimeException("Reserva não encontrada: " + reservaId));

        if (!reserva.getMoradorId().getId().equals(moradorId)) {
            throw new RuntimeException("Você não pode cancelar uma reserva de outro morador.");
        }

        reserva.cancelar();
        reservaRepository.save(reserva);
        log.info("Reserva cancelada. id={}", reservaId);
    }

    
    @Transactional(readOnly = true)
    public List<ReservaResponse> listar(UUID areaComumId, java.time.LocalDate data) {
        return reservaRepository.findByAreaComumEData(areaComumId, data).stream()
            .map(r -> ReservaResponse.fromEntity(r, null))
            .toList();
    }
}
