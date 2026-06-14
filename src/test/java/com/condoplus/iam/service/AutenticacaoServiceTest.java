package	com.condoplus.iam.service;
import	com.condoplus.iam.config.properties.JwtProperties;
import	com.condoplus.iam.config.properties.SecurityProperties;
import	com.condoplus.iam.domain.CredencialUsuario;
import	com.condoplus.iam.domain.Role;
import	com.condoplus.iam.domain.StatusCredencial;
import	com.condoplus.iam.domain.TipoRole;
import	com.condoplus.iam.dto.LoginRequest;
import	com.condoplus.iam.exception.CredencialBloqueadaException;
import	com.condoplus.iam.exception.CredenciaisInvalidasException;
import	com.condoplus.iam.repository.CredencialRepository;
import	org.junit.jupiter.api.BeforeEach;
import	org.junit.jupiter.api.Test;
import	org.junit.jupiter.api.extension.ExtendWith;
import	org.mockito.Mock;
import	org.mockito.junit.jupiter.MockitoExtension;
import	org.springframework.security.crypto.password.PasswordEncoder;
import	java.time.LocalDateTime;
import	java.util.Optional;
import	java.util.Set;
import	java.util.UUID;
import	static	org.assertj.core.api.Assertions.*;
import	static	org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)

class	AutenticacaoServiceTest	{
    @Mock	private	CredencialRepository	credencialRepository;
    @Mock	private	PasswordEncoder	passwordEncoder;
    @Mock	private	JwtService	jwtService;
    private	JwtProperties	jwtProperties;
    private	SecurityProperties	securityProperties;
    private	AutenticacaoService	autenticacaoService;
    @BeforeEach
    void	setup()	{
        jwtProperties	=	new	JwtProperties(
                "chave-de-teste-com-no-minimo-32-bytes-para-funcionar-ok",
                3600,	1800,	"test-issuer");
        securityProperties	=	new	SecurityProperties(5,	15);
        autenticacaoService	=	new	AutenticacaoService(
                credencialRepository,	passwordEncoder,	jwtService,
                jwtProperties,	securityProperties);
    }
    @Test
    void	deveAutenticarComCredenciaisCorretas()	{
        CredencialUsuario	cred	=	credencialAtiva();
        LoginRequest	req	=	new	LoginRequest("morador@condo.com",	"senha123");


        when(credencialRepository.findByEmail("morador@condo.com"))
                .thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("senha123",	cred.getSenhaHash()))
                .thenReturn(true);
        when(jwtService.gerarToken(cred)).thenReturn("token-fake");
        var	response	=	autenticacaoService.autenticar(req);
        assertThat(response.token()).isEqualTo("token-fake");
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
        verify(credencialRepository).save(cred);
        assertThat(cred.getTentativasFalhas()).isZero();
        assertThat(cred.getUltimoLogin()).isNotNull();
    }
    @Test
    void	deveRejeitarEmailInexistente()	{
        when(credencialRepository.findByEmail("naoexiste@condo.com"))
                .thenReturn(Optional.empty());
        LoginRequest	req	=	new	LoginRequest("naoexiste@condo.com",	"qualquer");
        assertThatThrownBy(()	->	autenticacaoService.autenticar(req))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(passwordEncoder,	never()).matches(any(),	any());
        verify(jwtService,	never()).gerarToken(any());
    }
    @Test
    void	deveIncrementarContadorEmSenhaErrada()	{
        CredencialUsuario	cred	=	credencialAtiva();
        LoginRequest	req	=	new	LoginRequest("morador@condo.com",	"errada");
        when(credencialRepository.findByEmail("morador@condo.com"))
                .thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("errada",	cred.getSenhaHash()))
                .thenReturn(false);
        assertThatThrownBy(()	->	autenticacaoService.autenticar(req))
                .isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(cred.getTentativasFalhas()).isEqualTo(1);
        verify(credencialRepository).save(cred);
    }
    @Test
    void	deveBloquearAposCincoTentativas()	{
        CredencialUsuario	cred	=	credencialAtiva();
        cred.setTentativasFalhas(4);
        LoginRequest	req	=	new	LoginRequest("morador@condo.com",	"errada");
        when(credencialRepository.findByEmail("morador@condo.com"))
                .thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("errada",	cred.getSenhaHash()))
                .thenReturn(false);
        assertThatThrownBy(()	->	autenticacaoService.autenticar(req))
                .isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(cred.getTentativasFalhas()).isEqualTo(5);
        assertThat(cred.getStatus()).isEqualTo(StatusCredencial.BLOQUEADO_TEMPORARIAMENTE);
        assertThat(cred.getBloqueadoAte()).isAfter(LocalDateTime.now());
    }
    @Test
    void	deveRejeitarCredencialBloqueadaManualmente()	{
        CredencialUsuario	cred	=	credencialAtiva();
        cred.setStatus(StatusCredencial.BLOQUEADO);
        LoginRequest	req	=	new	LoginRequest("morador@condo.com",	"senha123");
        when(credencialRepository.findByEmail("morador@condo.com"))
                .thenReturn(Optional.of(cred));
        assertThatThrownBy(()	->	autenticacaoService.autenticar(req))
                .isInstanceOf(CredencialBloqueadaException.class);
        verify(passwordEncoder,	never()).matches(any(),	any());
    }
    @Test
    void	deveDesbloquearAutomaticamenteAposExpirarJanela()	{
        CredencialUsuario	cred	=	credencialAtiva();
        cred.setStatus(StatusCredencial.BLOQUEADO_TEMPORARIAMENTE);
        cred.setBloqueadoAte(LocalDateTime.now().minusMinutes(1));		//	expirou
        cred.setTentativasFalhas(5);
        LoginRequest	req	=	new	LoginRequest("morador@condo.com",	"senha123");
        when(credencialRepository.findByEmail("morador@condo.com"))
                .thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("senha123",	cred.getSenhaHash()))
                .thenReturn(true);
        when(jwtService.gerarToken(cred)).thenReturn("token-fake");
        var	response	=	autenticacaoService.autenticar(req);
        assertThat(response.token()).isEqualTo("token-fake");
        assertThat(cred.getStatus()).isEqualTo(StatusCredencial.ATIVO);
        assertThat(cred.getTentativasFalhas()).isZero();
    }
    private	CredencialUsuario	credencialAtiva()	{
        Role	role	=	new	Role();
        role.setId(4L);
        role.setNome(TipoRole.MORADOR);
        return	CredencialUsuario.builder()
                .id(UUID.randomUUID())
                .email("morador@condo.com")
                .senhaHash("$2a$12$hashfake")
                .status(StatusCredencial.ATIVO)
                .tentativasFalhas(0)
                .roles(Set.of(role))
                .criadoEm(LocalDateTime.now())
                .build();
    }
}