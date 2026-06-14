package	com.condoplus.iam.service;

import	com.condoplus.iam.config.properties.JwtProperties;
import	com.condoplus.iam.domain.CredencialUsuario;
import	io.jsonwebtoken.Claims;
import	io.jsonwebtoken.ExpiredJwtException;
import	io.jsonwebtoken.Jwts;
import	io.jsonwebtoken.security.Keys;
import	lombok.RequiredArgsConstructor;
import	lombok.extern.slf4j.Slf4j;
import	org.springframework.stereotype.Service;
import	javax.crypto.SecretKey;
import	java.nio.charset.StandardCharsets;
import	java.time.Instant;
import	java.util.Date;
import	java.util.List;
import	java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j

public	class	JwtService	{
    private	final	JwtProperties	jwtProperties;

    public	String	gerarToken(CredencialUsuario	credencial)	{
        Instant	agora	=	Instant.now();
        Instant	expiracao	=	agora.plusSeconds(jwtProperties.expirationSeconds());
        List<String>	rolesComoStrings	=	credencial.getRoles().stream()
                .map(role	->	role.getNome().name())
                .toList();
        String	token	=	Jwts.builder()
                .subject(credencial.getId().toString())
                .claim("email",	credencial.getEmail())
                .claim("roles",	rolesComoStrings)
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .id(UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();
        log.debug("Token	JWT	gerado	para	credencial	id={}",	credencial.getId());
        return	token;
    }

    public	Claims	extrairClaims(String	token)	{
        return	Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public	Claims	extrairClaimsIgnorandoExpiracao(String	token)	{
        try	{
            return	extrairClaims(token);
        }	catch	(ExpiredJwtException	e)	{
            //	Token	está	expirado	mas	a	assinatura	era	válida.
            //	Retornamos	os	claims	mesmo	assim	para	que	o	refresh	decida.
            return	e.getClaims();
        }
    }

    public	long	segundosDesdeExpiracao(Claims	claims)	{
        long	agora	=	System.currentTimeMillis();
        long	exp	=	claims.getExpiration().getTime();
        return	(agora	-	exp)	/	1000;
    }
    private	SecretKey	getSigningKey()	{
        byte[]	keyBytes	=	jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        return	Keys.hmacShaKeyFor(keyBytes);
    }
}