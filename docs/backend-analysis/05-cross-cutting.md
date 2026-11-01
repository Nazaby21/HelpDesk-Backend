# Phase 5: Cross-Cutting Behavior & Platform Standards

This document analyzes the cross-cutting architectural concerns of the backend, including global error handling, pagination schemas, date/time formatting, real-time messaging, and external infrastructure integrations.

---

## 1. Global Exception Handling

The backend implements centralized exception handling via [GlobalExceptionHandler.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/exception/GlobalExceptionHandler.java) using Spring's `@RestControllerAdvice`.

### 1.1 Error Response Formats

The backend emits **two distinct error JSON schemas**, which requires Flutter mobile clients to implement defensive, polymorphic JSON deserialization.

#### Format A: Standard Error Object (`ErrorResponse`)
Returned for business, resource, access, and generic server errors:
```json
{
  "status": 404,
  "message": "Ticket not found",
  "timestamp": "2026-10-22T13:45:00.123456"
}
```

#### Format B: Validation Error Map
Returned specifically by `MethodArgumentNotValidException` when bean validation fails:
```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2026-10-22T13:45:00.123456",
  "errors": {
    "ticketTitle": "must not be blank",
    "priority": "must not be null"
  }
}
```

#### Format C: Ad-Hoc Controller Error Map
Returned directly from [ImageController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/ImageController.java#L25):
```json
{
  "error": "File is missings"
}
```

### 1.2 Exception Handler Mapping Table

| Exception Class | HTTP Status Code | Response Payload Structure | Root Cause in Backend |
| :--- | :--- | :--- | :--- |
| `BusinessException` | `400 BAD_REQUEST` | Format A (`ErrorResponse`) | Circular category parent assignment; category referenced by tickets on delete. |
| `MethodArgumentNotValidException` | `400 BAD_REQUEST` | Format B (Map with `errors` dictionary) | Triggered when `@Valid` request body violates bean constraints. |
| `AuthenticationException` | `401 UNAUTHORIZED` | Format A (`ErrorResponse`) | Bad login credentials or failed Spring Security authentication. |
| `AccessDeniedException` | `403 FORBIDDEN` | Format A (`ErrorResponse`) | Authenticated user lacks required role for the endpoint. |
| `ResourceNotFoundException` | `404 NOT_FOUND` | Format A (`ErrorResponse`) | Category ID not found during category operations. |
| `DataIntegrityViolationException` | `409 CONFLICT` | Format A (`ErrorResponse`) | Foreign key violation or database unique constraint failure. |
| `Exception` (Catch-All) | `500 INTERNAL_SERVER_ERROR` | Format A (`ErrorResponse`) | Any unhandled runtime exception. |

### 1.3 Critical Exception Handling Vulnerability: The "Generic 500" Mask
Throughout the codebase, services throw generic `RuntimeException`, `IllegalArgumentException`, and `IllegalStateException` rather than specialized domain exceptions:
- `UserServiceImpl`: Throws `RuntimeException("User not found")`, `RuntimeException("Department not found")`, `RuntimeException("Email already exists")`.
- `DepartmentServiceImpl`: Throws `RuntimeException("Department not found with id: ...")`.
- `TicketServiceImpl`: Throws `IllegalArgumentException("Completed ticket cannot be modified")` and `IllegalStateException("Deleted ticket cannot be updated")`.

Because neither `RuntimeException`, `IllegalArgumentException`, nor `IllegalStateException` are declared in [GlobalExceptionHandler.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/exception/GlobalExceptionHandler.java), they trigger `handleGenericException(Exception ex)`. 
**Impact on Mobile App**:
- Looking up a non-existent user or department returns `500 Internal Server Error` with `"An unexpected error occurred"` instead of `404 Not Found`.
- Attempting to register an existing email returns `500 Internal Server Error` instead of `409 Conflict`.
- Invalid status transitions return `500 Internal Server Error` instead of `400 Bad Request`.

---

## 2. Pagination Strategy & Schema

The API provides two divergent response patterns across its collections:

### 2.1 Paginated Collections (Spring Data `Page<T>`)
Used for ticket lists and activity feeds:
- `GET /api/v1/tickets`
- `GET /api/v1/tickets/my`
- `GET /api/v1/tickets/history`
- `GET /api/v1/tickets/activity`
- `GET /api/v1/debug/mytickets`

**Pagination Query Parameters**:
- `page` (int, default: `0`): Zero-indexed page number.
- `size` (int, default: `10`): Number of items per page.

**Response Structure (Spring Data `Page`)**:
```json
{
  "content": [ ... ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": { "empty": true, "sorted": false, "unsorted": true },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 3,
  "totalElements": 25,
  "last": false,
  "size": 10,
  "number": 0,
  "sort": { "empty": true, "sorted": false, "unsorted": true },
  "numberOfElements": 10,
  "first": true,
  "empty": false
}
```

### 2.2 Unpaginated Collections (Raw Arrays `List<T>`)
The following endpoints return raw JSON arrays with no pagination, offset, or limit support:
- `GET /api/v1/categories`: Dumps the entire category tree.
- `GET /api/v1/departments`: Dumps all departments.
- `GET /api/v1/users`: Dumps all non-deleted users in a single array.
- `GET /api/v1/tickets/{ticketId}/messages`: Dumps all chat messages for a ticket.
- `GET /api/v1/history/ticket/{ticketId}`: Dumps all history events.
- `GET /api/v1/status_log/ticket/{ticketId}`: Dumps all status changes.

> [!WARNING]
> **Mobile Performance Hazard**: In high-traffic deployments, `GET /api/v1/tickets/{ticketId}/messages` and `GET /api/v1/users` will transmit unbounded payloads. For long-running support tickets with hundreds of chat messages, loading the chat screen will degrade mobile memory and battery.

---

## 3. Date/Time & Timezone Serialization

### 3.1 Serialization Inconsistencies
The backend has mixed timestamp formats:

1. **Custom Formatted Date String (`"yyyy-MM-dd HH:mm:ss"`)**:
   - `TicketResponse.createdAt`
   - `TicketResponse.completedAt`
   - `AuditableCustom.createdAt`
   *Example*: `"2026-10-22 13:30:00"`
2. **Default ISO-8601 String (`"yyyy-MM-dd'T'HH:mm:ss"`)**:
   - `NotificationResponse.createdAt`
   - `UserResponse.createdAt` and `UserResponse.updatedAt`
   - `TicketHistoryResponse.performAt`
   - `TicketStatusLogResponse.changedAt`
   - `ChatMessage.timestamp`
   - `ErrorResponse.timestamp`
   *Example*: `"2026-10-22T13:30:00.123456"`

### 3.2 Timezone Handling
- All timestamps are stored and transmitted as `LocalDateTime` representing local server time.
- **No UTC offset (`Z` or `+00:00`) is included in responses**.
- If the backend server runs in UTC and the mobile client operates in UTC+7 (Jakarta/Bangkok), dates parsed without timezone offsets will display with 7 hours of skew in the mobile UI.

---

## 4. Real-Time Chat & STOMP Protocol Architecture

The real-time messaging subsystem is configured in [WebSocketConfig.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/config/WebSocketConfig.java):

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic");
    config.setApplicationDestinationPrefixes("/app");
}

@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-chat").setAllowedOriginPatterns("*").withSockJS();
}
```

### 4.1 WebSocket Architecture Specifications
- **Protocol**: STOMP over WebSocket.
- **Handshake URL**: `ws://<backend-host>:8080/ws-chat/websocket` (or via SockJS framing at `/ws-chat`).
- **In-Memory Broker**: Simple broker enabled on prefix `/topic`.
- **Application Prefix**: `/app`.
- **Send Destination**: `/app/chat` mapped to [ChatController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/ChatController.java#L23).
- **Subscribe Destination**: `/topic/ticket/{ticketId}`.

### 4.2 WebSocket Authentication Gap
- Notice in [SecurityConfig.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/security/SecurityConfig.java#L72):
  `.requestMatchers("/ws-chat/**").permitAll()`
- **There is NO WebSocket handshake interceptor or STOMP `ChannelInterceptor` validating JWT tokens**.
- Any client can connect to the WebSocket and subscribe to `/topic/ticket/{anyTicketId}` to eavesdrop on live support conversations without authenticating.

---

## 5. Background Jobs, Caching & External Integrations

| Subsystem | Status in Codebase | Implementation Details |
| :--- | :--- | :--- |
| **Caching** | **Not implemented** | No `@EnableCaching`, no Redis, no Caffeine in `pom.xml`. Every request queries PostgreSQL directly. |
| **Rate Limiting** | **Not implemented** | No Bucket4j or token bucket filters. Endpoints are vulnerable to brute-force attacks. |
| **Scheduled Tasks** | **Not implemented** | No `@Scheduled` tasks or cron background workers. |
| **Async Execution** | **Not implemented** | No `@Async` or `ThreadPoolTaskExecutor`. All requests are synchronous on Tomcat worker threads. |
| **File Storage** | **Supabase Storage** | REST API calls via Apache HttpClient 5 in [SupabaseStorageService.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/service/SupabaseStorageService.java). |
| **Push Notifications**| **Not implemented** | No Firebase Cloud Messaging (FCM) or Apple Push Notification Service (APNS). |
| **Email Service** | **Not implemented** | No `JavaMailSender` configured. |
| **Internationalization**| **Not implemented** | No `MessageSource` or `messages_*.properties`. All UI messages are hardcoded in English. |
