package com.condoplus.condominio.convivencia.repository;

import com.condoplus.condominio.convivencia.domain.Comunicado;
import com.condoplus.condominio.convivencia.domain.PublicoAlvo;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface ComunicadoRepository extends CrudRepository<Comunicado, UUID> {

    
    @Query("SELECT * FROM condominio.comunicado ORDER BY data_publicacao DESC LIMIT :limit OFFSET :offset")
    List<Comunicado> findRecentes(int limit, int offset);

    
    @Query("SELECT * FROM condominio.comunicado WHERE publico_alvo = :publicoAlvo ORDER BY data_publicacao DESC")
    List<Comunicado> findByPublicoAlvo(PublicoAlvo publicoAlvo);
}
