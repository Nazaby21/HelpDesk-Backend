package com.zaby.helpdesk.config;

import com.zaby.helpdesk.security.CustomUserDetailsService;
import com.zaby.helpdesk.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // broadcast messages
        config.setApplicationDestinationPrefixes("/app"); // client sends here
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Raw WebSocket endpoint for Flutter mobile and native clients
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*");

        // SockJS fallback endpoint for web browser compatibility
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            String email = jwtService.extractEmail(token);
                            if (email != null) {
                                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                                if (jwtService.isTokenValid(token, userDetails)) {
                                    UsernamePasswordAuthenticationToken authentication =
                                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                    accessor.setUser(authentication);
                                    SecurityContextHolder.getContext().setAuthentication(authentication);
                                } else {
                                    throw new AuthenticationCredentialsNotFoundException("Invalid JWT token");
                                }
                            } else {
                                throw new AuthenticationCredentialsNotFoundException("Invalid JWT token");
                            }
                        } catch (Exception e) {
                            throw new AuthenticationCredentialsNotFoundException("Authentication failed: " + e.getMessage());
                        }
                    } else {
                        throw new AuthenticationCredentialsNotFoundException("Missing JWT Bearer token");
                    }
                }
                return message;
            }
        });
    }
}
