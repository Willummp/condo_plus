package com.condoplus.iam.service;

import com.condoplus.iam.controller.AutenticacaoController;
import	com.condoplus.iam.dto.LoginRequest;
import	com.condoplus.iam.dto.TokenResponse;
import	com.condoplus.iam.exception.CredenciaisInvalidasException;
import	com.fasterxml.jackson.databind.ObjectMapper;
import	org.junit.jupiter.api.Test;
import	org.springframework.beans.factory.annotation.Autowired;
import	org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import	org.springframework.boot.test.mock.mockito.MockBean;
import	org.springframework.context.annotation.Import;
import	org.springframework.http.MediaType;
import	org.springframework.test.web.servlet.MockMvc;
import	static	org.mockito.Mockito.*;
import	static	org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import	static	org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers	=	AutenticacaoController.class)
@Import(com.condoplus.iam.config.SecurityConfig.class)
class	AutenticacaoControllerTest	{
    @Autowired	private	MockMvc	mockMvc;
    @Autowired	private	ObjectMapper	objectMapper;
    @MockBean	private	AutenticacaoService	autenticacaoService;
    @Test
    void	loginComSucessoDeveRetornarToken()	throws	Exception	{
        when(autenticacaoService.autenticar(any()))
                .thenReturn(new	TokenResponse("token-jwt-fake",	3600L));
        LoginRequest	req	=	new	LoginRequest("morador@condo.com",	"senha123");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-jwt-fake"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }
    @Test
    void	loginComCredenciaisInvalidasDeveRetornar401()	throws	Exception	{
        when(autenticacaoService.autenticar(any()))
                .thenThrow(new	CredenciaisInvalidasException());
        LoginRequest	req	=	new	LoginRequest("morador@condo.com",	"errada");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciais	inválidas"));
    }
    @Test
    void	loginComEmailInvalidoDeveRetornar400()	throws	Exception	{
        LoginRequest	req	=	new	LoginRequest("nao-é-email",	"senha123");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}