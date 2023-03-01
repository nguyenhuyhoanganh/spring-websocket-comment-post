package com.involveininnovation.chat.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.involveininnovation.chat.entity.User;
import com.involveininnovation.chat.repo.UserRepo;
import com.involveininnovation.chat.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    // AbstractSecurityWebSocketMessageBrokerConfigurer extends AbstractWebSocketMessageBrokerConfigurer implements
    // WebSocketMessageBrokerConfigurer
    @Autowired
    private UserRepo repo;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("http://localhost:3000").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/comment");
        registry.enableSimpleBroker("/post", "/user");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null &&
                        (StompCommand.CONNECT.equals(accessor.getCommand()) ||
                                StompCommand.SEND.equals(accessor.getCommand()))) {
                    String token = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                        // get username from token
                        DecodedJWT decodedJWT = JwtUtils.decodeToken(token);
                        String username = decodedJWT.getSubject();
                        // find user by username
                        User user = repo.findByUsername(username);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, null);
                        accessor.setUser(authentication);
                    }
                }
                return message;
            }
        });
    }

}
