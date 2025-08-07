package org.example.opencvdemo.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.opencvdemo.controllers.AuthenticationController;
import org.example.opencvdemo.exception.ApiRequestException;
import org.example.opencvdemo.helper.AuthenticationRequest;
import org.example.opencvdemo.helper.RegisterRequest;
import org.example.opencvdemo.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;


import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;


import java.util.Collection;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private UserService userService;
    @Autowired
    ObjectMapper objectMapper;

    RegisterRequest registerRequest;
    AuthenticationRequest authenticationRequest;

    @BeforeEach
    void setUp() {

        registerRequest = RegisterRequest.builder().username("TestUser").password("password").build();
        authenticationRequest = AuthenticationRequest.builder().username("TestUser").password("password").build();
    }

    @Test
    public void AuthenticationControllerTest_Register_ReturnsSuccessResult() throws Exception {

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("User created Successfully"));

        verify(userService).createUser(any(RegisterRequest.class),eq("ROLE_USER"));

    }

    @Test
    public void AuthenticationControllerTest_register_ReturnsServiceException() throws Exception {

        doThrow(new ApiRequestException("Username already exists")).when(userService).createUser(any(RegisterRequest.class),any());


        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void AuthenticationControllerTest_create_ReturnsSuccessResult() throws Exception {
        mockMvc.perform(post("/auth/admin/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin created Successfully"));

        verify(userService).createUser(any(RegisterRequest.class),eq("ROLE_ADMIN"));

    }

    @Test
    public void AuthenticationControllerTest_create_ReturnsServiceException() throws Exception {

        doThrow(new ApiRequestException("Username already exists")).when(userService).createUser(any(RegisterRequest.class),any());


        mockMvc.perform(post("/auth/admin/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

    }


    @Test
    public void AuthenticationControllerTest_login_ReturnsSessionIdAndRole() throws Exception {

        Authentication mockAuth = new UsernamePasswordAuthenticationToken(
                "TestUser", "password",Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));


        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);


        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect( jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.role").value("[ROLE_USER]"));


        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

    }


    @Test
    public void AuthenticationControllerTest_login_ReturnsBadCredentialsException() throws Exception {

        Authentication mockAuth = new UsernamePasswordAuthenticationToken(
                "TestUser", "password");


        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("Invalid username or password"));


        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .with(csrf()))
                        .andExpect(status().isBadRequest())
                        .andExpect( jsonPath("$.sessionId").isEmpty())
                        .andExpect(jsonPath("$.role").isEmpty());


    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

    }

    @Test
    public void AuthenticationControllerTest_logout_ReturnsSuccess() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .with(csrf())).andExpect(status().isOk()).andExpect(content().string("Logout successful"));

    }

}
