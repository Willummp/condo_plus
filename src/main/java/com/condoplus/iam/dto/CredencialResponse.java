package	com.condoplus.iam.dto;

import	com.condoplus.iam.domain.StatusCredencial;
import	com.condoplus.iam.domain.TipoRole;
import	java.time.LocalDateTime;
import	java.util.Set;
import	java.util.UUID;

public	record	CredencialResponse(
        UUID	id,
        String	email,
        StatusCredencial	status,
        Set<TipoRole>	roles,
        LocalDateTime	criadoEm,
        LocalDateTime	ultimoLogin
)	{}