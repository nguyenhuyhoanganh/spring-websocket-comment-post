package com.involveininnovation.chat.utils;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.involveininnovation.chat.entity.User;
import com.involveininnovation.chat.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    @Autowired
    private UserRepo repository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            System.out.println("1. header:" + servletRequest.getHeaders());

            // get token from authorization header
            String token = servletRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            System.out.println("2. token: " + token);
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                System.out.println("3. jwt: " + token);
                // get username from token
                DecodedJWT decodedJWT = JwtUtils.decodeToken(token);
                String username = decodedJWT.getSubject();
                System.out.println("4. username: " + username);
                // find user by email
                User user = repository.findByUsername(username);
                System.out.println("5. user: " + user);
                CustomUserDetails userDetails = new CustomUserDetails(user);

                // set user to security context
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        return super.beforeHandshake(request, response, wsHandler, attributes);
    }
}

