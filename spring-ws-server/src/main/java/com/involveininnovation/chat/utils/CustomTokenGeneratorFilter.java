package com.involveininnovation.chat.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.involveininnovation.chat.entity.User;
import com.involveininnovation.chat.model.ResponseDTO;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StreamUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomTokenGeneratorFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;

    public CustomTokenGeneratorFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @SneakyThrows
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (!request.getMethod().equals("POST"))
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        try {
            byte[] inputStreamBytes = StreamUtils.copyToByteArray(request.getInputStream());
            Map<String, String> jsonRequest = new ObjectMapper().readValue(inputStreamBytes, Map.class);
            String username = jsonRequest.get("username");
            String password = jsonRequest.get("password");
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            return authenticationManager.authenticate(authenticationToken);
        } catch (Exception exception) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            ResponseDTO<?> responseBody = ResponseDTO.builder().message("Email or password is incorrect").code(HttpStatus.FORBIDDEN.value()).build();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(response.getOutputStream(), responseBody);
        }
        return null;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        String subject = authentication.getName();
        String issuer = request.getRequestURL().toString();
        String roles = JwtUtils.populateAuthorities(authentication.getAuthorities());
        Map<String, String> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("type", "access_token");
        String accessToken = JwtUtils.generateToken(subject, SecurityConstants.ACCESS_TOKEN_LIFE_TIME, issuer, claims);

        Map<String, Object> data = new HashMap<>();
        data.put("access_token", accessToken);
        data.put("user", user);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.OK.value());
        ResponseDTO<?> responseBody = ResponseDTO.builder().data(data).code(HttpStatus.OK.value()).build();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(response.getOutputStream(), responseBody);
    }
}

