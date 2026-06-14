package	com.condoplus.iam.service;

import	com.condoplus.iam.config.properties.JwtProperties;
import	com.condoplus.iam.domain.CredencialUsuario;
import	com.condoplus.iam.domain.Role;
import	com.condoplus.iam.domain.StatusCredencial;
import	com.condoplus.iam.domain.TipoRole;
import	io.jsonwebtoken.Claims;
import	io.jsonwebtoken.ExpiredJwtException;
import	org.junit.jupiter.api.BeforeEach;
import	org.junit.jupiter.api.Test;
import	java.util.Set;
import	java.util.UUID;
import	static	org.assertj.core.api.Assertions.*;

class	JwtServiceTest	{
    private	JwtService	jwtService;
    @BeforeEach
    void	setup()	{
        JwtProperties	props	=	new	JwtProperties(
                "chave-de-teste-com-no-minimo-32-bytes-para-funcionar-ok",
                3600,
                1800,
                "test-issuer"
        );
        jwtService	=	new	JwtService(props);
    }
    @Test
    void	deveGerarTokenComClaimsCorretos()	{
        CredencialUsuario	cred	=	CredencialUsuario.builder()
                .id(UUID.randomUUID())
                .email("teste@condo.com")
                .senhaHash("hash-fake")
                .status(StatusCredencial.ATIVO)
                .roles(Set.of(criaRole(TipoRole.MORADOR)))
                .build();
        String	token	=	jwtService.gerarToken(cred);
        Claims	claims	=	jwtService.extrairClaims(token);
        assertThat(claims.getSubject()).isEqualTo(cred.getId().toString());
        assertThat(claims.get("email")).isEqualTo("teste@condo.com");
        assertThat(claims.get("roles",	java.util.List.class))
                .containsExactly("MORADOR");
        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
    }
    @Test
    void	deveRejeitarTokenAdulterado()	{
        CredencialUsuario	cred	=	CredencialUsuario.builder()
                .id(UUID.randomUUID())
                .email("teste@condo.com")
                .senhaHash("hash-fake")
                .status(StatusCredencial.ATIVO)
                .roles(Set.of(criaRole(TipoRole.MORADOR)))
                .build();
        String	token	=	jwtService.gerarToken(cred);
        //	Adultera	o	último	caractere
        String	tokenAdulterado	=	token.substring(0,	token.length()	-	1)	+
                (token.endsWith("a")	?	"b"	:	"a");
        assertThatThrownBy(()	->	jwtService.extrairClaims(tokenAdulterado))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
    @Test
    void	extrairIgnorandoExpiracaoDeveLidarComTokenExpirado()	{
        JwtProperties	propsExpirado	=	new	JwtProperties(
                "chave-de-teste-com-no-minimo-32-bytes-para-funcionar-ok",
                1,	1800,	"test-issuer"
        );
        JwtService	srv	=	new	JwtService(propsExpirado);
        CredencialUsuario	cred	=	CredencialUsuario.builder()
                .id(UUID.randomUUID())
                .email("expirado@condo.com")
                .senhaHash("hash-fake")
                .status(StatusCredencial.ATIVO)
                .roles(Set.of(criaRole(TipoRole.MORADOR)))
                .build();
        String	token	=	srv.gerarToken(cred);
        try	{	Thread.sleep(2000);	}	catch	(InterruptedException	ignored)	{}
        assertThatThrownBy(()	->	srv.extrairClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
        Claims	claims	=	srv.extrairClaimsIgnorandoExpiracao(token);
        assertThat(claims.get("email")).isEqualTo("expirado@condo.com");
    }
    private	Role	criaRole(TipoRole	tipo)	{
        Role	r	=	new	Role();
        r.setId((long)	tipo.ordinal()	+	1);
        r.setNome(tipo);
        return	r;
    }
}