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

/**
 * Serviço responsável por gerenciar a criação, cancelamento e listagem de Reservas.
 * 
 * <p>Este serviço gerencia a concorrência e consistência de agendamento de áreas comuns,
 * aplicando mecanismos transacionais para evitar conflitos de reservas no mesmo período.
 *
 * <p><b>Estratégia de Concorrência:</b>
 * Para a criação de reservas, emprega-se o nível de isolamento {@code Isolation.SERIALIZABLE} (SSI no PostgreSQL) 
 * para impedir condições de corrida (Race Conditions) onde dois moradores poderiam reservar a mesma área 
 * no mesmo horário simultaneamente. Se houver concorrência conflitante, o banco aborta a transação secundária, 
 * que é convertida em um erro tratado amigavelmente pelo sistema.
 * 
 * <p>Anotações importantes:
 * <ul>
 *   <li>{@code @Service} — Declara esta classe como um componente de serviço gerenciado pelo Spring IoC, habilitando a injeção de dependências.</li>
 *   <li>{@code @Slf4j} — Injeta automaticamente um Logger SLF4J (Logback) sob o atributo {@code log} para registro de auditoria e depuração local.</li>
 * </ul>
 */
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

    /**
     * Realiza o agendamento (reserva) de uma área comum de forma consistente e atômica.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(isolation = Isolation.SERIALIZABLE)} — Nível de isolamento mais alto do banco (SSI no PostgreSQL). Garante que se duas requisições concorrentes tentarem criar reservas sobrepostas simultaneamente, uma delas seja abortada e revertida, mantendo a consistência do banco de dados 100% livre de conflitos.</li>
     * </ul>
     * 
     * @param req DTO contendo a área comum de interesse, data e período da reserva.
     * @param moradorId ID único (UUID) do morador que está solicitando a reserva.
     * @return ReservaResponse contendo a confirmação do agendamento.
     * @throws AreaComumNaoEncontradaException se a área comum especificada não existir.
     * @throws AreaComumIndisponivelException se a área comum estiver desativada/inativa.
     * @throws ConflitoReservaException se já houver reserva confirmada no mesmo período.
     */
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

    /**
     * Cancela uma reserva ativa sob a responsabilidade do próprio morador solicitante.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional} — Abre uma transação comum (READ COMMITTED) para persistir o cancelamento da reserva com atomicidade.</li>
     * </ul>
     * 
     * @param reservaId ID único (UUID) da reserva que será cancelada.
     * @param moradorId ID único (UUID) do morador autenticado que solicitou a operação.
     * @throws RuntimeException se a reserva não existir ou pertencer a outro morador.
     */
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

    /**
     * Lista todas as reservas confirmadas de uma área comum em uma data específica.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Transactional(readOnly = true)} — Otimiza o acesso a dados de leitura, desabilitando verificações de dirty-checking e locks de escrita desnecessários no banco.</li>
     * </ul>
     * 
     * @param areaComumId ID da área comum que se deseja consultar.
     * @param data Data de interesse para o filtro de reservas.
     * @return Lista contendo os DTOs de reservas encontradas.
     */
    @Transactional(readOnly = true)
    public List<ReservaResponse> listar(UUID areaComumId, java.time.LocalDate data) {
        return reservaRepository.findByAreaComumEData(areaComumId, data).stream()
            .map(r -> ReservaResponse.fromEntity(r, null))
            .toList();
    }
}
