package	com.condoplus.iam.controller;

import	com.condoplus.iam.dto.AlteracaoStatusRequest;
import	com.condoplus.iam.dto.CredencialResponse;
import	com.condoplus.iam.dto.NovaCredencialRequest;
import	com.condoplus.iam.service.CredencialService;
import	jakarta.validation.Valid;
import	lombok.RequiredArgsConstructor;
import	org.springframework.http.HttpStatus;
import	org.springframework.http.ResponseEntity;
import	org.springframework.security.access.prepost.PreAuthorize;
import	org.springframework.web.bind.annotation.*;
import	java.net.URI;
import	java.util.UUID;

@RestController
@RequestMapping("/credenciais")
@RequiredArgsConstructor
public	class	CredencialController	{
    private	final	CredencialService	credencialService;
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN',	'SINDICO')")
    public	ResponseEntity<CredencialResponse>	criar(
            @Valid	@RequestBody	NovaCredencialRequest	req)	{
        CredencialResponse	criada	=	credencialService.criar(req);
        URI	location	=	URI.create("/credenciais/"	+	criada.id());
        return	ResponseEntity.created(location).body(criada);
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN',	'SINDICO')")
    public	ResponseEntity<CredencialResponse>	buscar(@PathVariable	UUID	id)	{
        return	ResponseEntity.ok(credencialService.buscar(id));
    }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public	ResponseEntity<Void>	alterarStatus(
            @PathVariable	UUID	id,
            @Valid	@RequestBody	AlteracaoStatusRequest	req)	{
        credencialService.alterarStatus(id,	req);
        return	ResponseEntity.noContent().build();
    }
}