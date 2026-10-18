package com.raysi.sockets.phase5_spring_websocket.codes;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Author: raysi
 * Package: com.raysi.sockets.phase5_spring_websocket.codes
 * 
 * Production-Grade WebSocket Configuration.
 * Configures the STOMP endpoint, SockJS fallback, and message broker routing topology.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker options.
     * Defines client-to-server routing prefixes and broker-to-client broadcast destinations.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable an in-memory STOMP broker for broadcast/pub-sub topics (/topic) and point-to-point queues (/queue)
        config.enableSimpleBroker("/topic", "/queue");

        // Application prefix for messages routed directly to @MessageMapping handler methods in @Controller
        config.setApplicationDestinationPrefixes("/app");

        // Destination prefix used for user-specific private messaging (e.g., convertAndSendToUser)
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints mapping to specific URLs for initial WebSocket handshakes.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Handshake endpoint clients connect to (e.g. ws://localhost:8080/ws)
        // .withSockJS() provides fallback transport options (XHR streaming, XHR polling) for legacy proxies
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
