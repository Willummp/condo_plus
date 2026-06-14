/*
package	com.condoplus.iam.service;
import	com.condoplus.events.Evento;
import	lombok.RequiredArgsConstructor;
import	lombok.extern.slf4j.Slf4j;
import	org.springframework.kafka.core.KafkaTemplate;
import	org.springframework.scheduling.annotation.Async;
import	org.springframework.stereotype.Component;
import	java.util.concurrent.CompletableFuture;
@Component
@RequiredArgsConstructor
@Slf4j
public	class	EventoPublicador	{
    private	final	KafkaTemplate<String,	Evento>	kafkaTemplate;
    @Async
    public	void	publicar(String	topico,	String	chave,	Evento	evento)	{
        CompletableFuture<?>	future	=	kafkaTemplate.send(topico,	chave,	evento);
        future.whenComplete((result,	ex)	->	{
            if	(ex	!=	null)	{
                log.error("Falha	ao	publicar	evento.	topico={}	eventId={}",
                        topico,	evento.getEventId(),	ex);
            }	else	{
                log.debug("Evento	publicado.	topico={}	eventId={}",
                        topico,	evento.getEventId());
            }
        });
    }
}

 */