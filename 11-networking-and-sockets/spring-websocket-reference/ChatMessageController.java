package com.raysi.sockets.phase5_spring_websocket.codes;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * Author: raysi
 * Package: com.raysi.sockets.phase5_spring_websocket.codes
 * 
 * Handles incoming STOMP messages routed to application destination prefixes
 * and broadcasts responses to destination topics.
 */
@Controller
public class ChatMessageController {

    // Simple DTO payload for chat messages using Java Record
    public record ChatMessage(String sender, String content) {}

    /**
     * When a client sends a message to "/app/chat", it routes to this method.
     * The return value is broadcasted automatically to everyone subscribed to "/topic/public".
     */
    @MessageMapping("/chat")
    @SendTo("/topic/public")
    public ChatMessage broadcastMessage(@Payload ChatMessage message) {
        return new ChatMessage(message.sender(), message.content());
    }
}
