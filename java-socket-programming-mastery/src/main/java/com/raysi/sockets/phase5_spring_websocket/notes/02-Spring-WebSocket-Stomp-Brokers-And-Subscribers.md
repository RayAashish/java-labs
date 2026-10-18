# 02: Spring WebSocket STOMP Brokers, Routing & Subscribers
**Author:** `raysi`  
**Package:** `com.raysi.sockets.phase5_spring_websocket`  
**Framework:** Spring Boot WebSockets (`org.springframework.boot:spring-boot-starter-websocket`)

---

# 1. Spring STOMP Internal Architecture & Message Flow

When Spring enables WebSocket messaging via `@EnableWebSocketMessageBroker`, it sets up a message-driven pipeline powered by Spring Integration channels.

```text
 +-----------------------------------------------------------------------------------------+
 |                                  SPRING APPLICATION ENGINE                              |
 |                                                                                         |
 |   [ WebSocket Client ]                                                                  |
 |            |                                                                            |
 |            | STOMP Frames (SEND / SUBSCRIBE / CONNECT)                                  |
 |            v                                                                            |
 |   +---------------------------------------------------------------------------------+   |
 |   | clientInboundChannel (ThreadPoolExecutor)                                       |   |
 |   +---------------------------------------------------------------------------------+   |
 |            |                                                                            |
 |            +-------------------------------+------------------------------+             |
 |            | destination starts with /app  | destination starts with      |             |
 |            |                               | /topic or /queue             |             |
 |            v                               v                              v             |
 |   +------------------------+      +--------------------+      +---------------------+   |
 |   | SimpAnnotationMethod   |      | SimpleBroker       |      | UserDestination     |   |
 |   | MessageHandler         |      | MessageHandler     |      | MessageHandler      |   |
 |   | (@MessageMapping)      |      | (In-Memory PubSub) |      | (/user/{name}/...)  |   |
 |   +------------------------+      +--------------------+      +---------------------+   |
 |            |                               ^                              |             |
 |            | Return value / @SendTo        |                              |             |
 |            v                               |                              |             |
 |   +----------------------------------------+                              |             |
 |   | brokerChannel                          |                              |             |
 |   +----------------------------------------+                              |             |
 |            |                                                              |             |
 |            +-------------------------------+------------------------------+             |
 |                                            |                                            |
 |                                            v                                            |
 |   +---------------------------------------------------------------------------------+   |
 |   | clientOutboundChannel (ThreadPoolExecutor)                                      |   |
 |   +---------------------------------------------------------------------------------+   |
 |            |                                                                            |
 |            v STOMP MESSAGE Frames                                                       |
 |   [ Subscribed WebSocket Clients ]                                                      |
 +-----------------------------------------------------------------------------------------+
```

### The Three Fundamental Message Channels:
1. **`clientInboundChannel`**: Carries incoming STOMP messages received from WebSocket clients. Worker threads parse the frames and dispatch them to the appropriate handler.
2. **`clientOutboundChannel`**: Carries outbound STOMP messages dispatched from the server or broker to connected clients.
3. **`brokerChannel`**: Carries messages sent from within the application (e.g., from `@Controller` return values or `SimpMessagingTemplate`) directly to the message broker.

---

# 2. Detailed Routing Flow: `/app/...` vs `/topic/...`

The prefix declared in `WebSocketConfig` strictly dictates how messages travel through the system:

```text
CLIENT SENDS FRAME:
========================================================================================

Case 1: Destination = "/app/chat"
   1. Frame arrives on `clientInboundChannel`.
   2. Matched by `setApplicationDestinationPrefixes("/app")`.
   3. Strips prefix "/app" -> invokes `@MessageMapping("/chat")` in `ChatMessageController`.
   4. Controller processes business logic (validation, DB persistence, enrichment).
   5. Controller method returns a `ChatMessage` with `@SendTo("/topic/public")`.
   6. Return value is wrapped in a STOMP MESSAGE frame and published to `brokerChannel`.
   7. `SimpleBrokerMessageHandler` looks up all active subscriptions for `/topic/public`.
   8. Broker forwards duplicate frames to `clientOutboundChannel` for each subscriber.

----------------------------------------------------------------------------------------

Case 2: Destination = "/topic/public" (Client SUBSCRIBE)
   1. Client sends STOMP frame: `SUBSCRIBE /topic/public` with `id:sub-101`.
   2. Frame arrives on `clientInboundChannel`.
   3. Since it starts with `/topic`, it bypasses `@Controller` methods completely.
   4. Delivered directly to `SimpleBrokerMessageHandler`.
   5. Broker registers the client's `SessionId` and `SubscriptionId` in its in-memory subscription registry.
   6. Any subsequent message published to `/topic/public` is immediately broadcasted to this client.
```

---

# 3. Simple In-Memory Broker vs Full External STOMP Relay

Spring provides two distinct broker models:

### 1. In-Memory Simple Broker (`config.enableSimpleBroker("/topic", "/queue")`)
- **How it works:** Spring maintains a thread-safe registry (`DefaultSubscriptionRegistry`) of connected sessions and active subscriptions in JVM memory.
- **Pros:** Zero external dependencies, ultra-fast, zero deployment complexity.
- **Cons:** 
  - **Single Node Only:** Cannot scale horizontally across multiple instances (Instance A does not know about subscribers on Instance B).
  - No message persistence or dead-letter queues.
  - Memory consumption scales with the number of concurrent sessions and queued messages.

### 2. External STOMP Broker Relay (RabbitMQ, ActiveMQ)
- Configured via `config.enableStompBrokerRelay("/topic", "/queue").setRelayHost("rabbitmq.internal")`.
- Spring establishes a dedicated TCP connection pool to the message broker.
- **Horizontal Scalability:** All Spring instances forward subscriptions and messages to RabbitMQ. If Client 1 is connected to Instance A and Client 2 is connected to Instance B, RabbitMQ routes messages between them seamlessly.

---

# 4. Point-to-Point & User-Specific Messaging (`/user/...`)

In addition to broadcast rooms (`/topic`), real-time systems require private, 1-to-1 notifications (e.g., direct messaging, individual order status).

### How Spring Routes to a Specific User:
When a client subscribes to `/user/queue/notifications`:
1. `UserDestinationMessageHandler` intercepts the subscription.
2. It detects the client's authenticated `Principal.getName()` (e.g., `raysi`) or unique WebSocket `SessionId`.
3. It transforms the destination into a unique, session-scoped destination:
   $$\text{/user/queue/notifications} \longrightarrow \text{/queue/notifications-user-x89f2a}$$
4. The client listens on this dynamically isolated queue.

### Sending User-Specific Messages Programmatically:
```java
@Autowired
private SimpMessagingTemplate messagingTemplate;

public void notifyUser(String username, AlertNotification alert) {
    // Sends to the private destination of the specified user
    messagingTemplate.convertAndSendToUser(
        username,
        "/queue/notifications",
        alert
    );
}
```

---

# 5. Connection Lifecycle & Event Interceptors

Spring publishes application events for WebSocket session transitions:

| Spring Event | Trigger Point | Production Use Case |
| :--- | :--- | :--- |
| `SessionConnectEvent` | Received initial STOMP `CONNECT` frame | Validate authentication tokens before allowing session. |
| `SessionConnectedEvent` | STOMP connection negotiated & `CONNECTED` sent | Mark user as "ONLINE" in presence database / Redis. |
| `SessionSubscribeEvent` | Received STOMP `SUBSCRIBE` frame | Authorize subscription permissions on private channels. |
| `SessionUnsubscribeEvent` | Received STOMP `UNSUBSCRIBE` frame | Clean up subscription-level metrics. |
| `SessionDisconnectEvent` | TCP socket closed or STOMP `DISCONNECT` received | Mark user as "OFFLINE", broadcast leave event to rooms. |

### Intercepting Handshakes & JWT Authentication
Because standard WebSockets do not permit custom headers in browser JavaScript handshakes, enterprise architectures inspect the STOMP `CONNECT` frame using a `ChannelInterceptor`:

```java
@Component
public class AuthChannelInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authToken = accessor.getFirstNativeHeader("Authorization");
            // Validate JWT and populate accessor.setUser(authenticatedPrincipal)
        }
        return message;
    }
}
```
