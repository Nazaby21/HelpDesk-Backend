# Minimal Backend Changes Log (`docs/backend-changes.md`)

> **Generated**: 2026-11-01
> **Policy**: Per project rules, backend is the source of truth. No existing endpoints, paths, or response fields were renamed or removed. Only minimal fixes required for client features to work were applied.

---

## 1. Fix `TicketServiceImpl.updateTicket` Discarding `imageUrls` (`PUT /api/v1/tickets/{id}`)
- **File**: [`src/main/java/com/zaby/helpdesk/service/impl/TicketServiceImpl.java`](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/service/impl/TicketServiceImpl.java)
- **Why**: `TicketRequest` accepts `List<String> imageUrls`, and Web Frontend (`EditTicketView.tsx`) sends `imageUrls` when editing a ticket's attachments, but `TicketServiceImpl.updateTicket` omitted `ticket.setImageUrls(...)`, silently discarding all attachment additions or removals.
- **Change**: Added `if (ticketRequest.imageUrls() != null) { ticket.setImageUrls(new ArrayList<>(ticketRequest.imageUrls())); }` inside `updateTicket`.

## 2. Add REST Chat Message Creation Endpoint (`POST /api/v1/tickets/{ticketId}/messages`)
- **File**: [`src/main/java/com/zaby/helpdesk/controller/ChatController.java`](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/ChatController.java)
- **Why**: `ChatController` previously only exposed `@MessageMapping("/chat")` (STOMP) and `GET /api/v1/tickets/{ticketId}/messages`. The Flutter mobile app (`TicketDetailScreen`) uses HTTP via `TicketRepository` and was unable to persist chat messages via REST, while Web Frontend also benefits from a reliable REST fallback when WebSocket is disconnected.
- **Change**: Added `@PostMapping("/api/v1/tickets/{ticketId}/messages")` which sets `ticketId`, populates `sender` from the authenticated `CustomUserDetails` if blank, sets `timestamp = LocalDateTime.now()`, saves to `chatMessageRepository`, broadcasts to `/topic/ticket/{ticketId}` via `simpMessagingTemplate`, and returns `201 Created` with the saved `ChatMessage`.

## 3. Add Self-Profile Update Endpoint (`PUT /api/v1/users/me`) & Guard Nullable `departmentId`
- **Files**:
  - [`src/main/java/com/zaby/helpdesk/controller/UserController.java`](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/UserController.java)
  - [`src/main/java/com/zaby/helpdesk/service/UserService.java`](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/service/UserService.java)
  - [`src/main/java/com/zaby/helpdesk/service/impl/UserServiceImpl.java`](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/service/impl/UserServiceImpl.java)
  - [`src/main/java/com/zaby/helpdesk/security/SecurityConfig.java`](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/security/SecurityConfig.java)
- **Why**:
  1. Both Web Frontend (`/pages/settings` -> `PersonalInfoForm` & `UploadPhotoForm`) and Flutter Mobile (`ProfileScreen`) provide profile management for the logged-in user (`USER`, `TECHNICIAN`, `ADMIN`), whereas `PUT /api/v1/users/{id}` is restricted to `ROLE_ADMIN`.
  2. `UserServiceImpl.createUser` and `updateUser` called `departmentRepository.findById(userRequest.departmentId())` without a null check, throwing `IllegalArgumentException: The given id must not be null` when `departmentId` is omitted.
- **Change**:
  - Added `PUT /api/v1/users/me` in `UserController` (permitted to `.authenticated()` in `SecurityConfig`) allowing any authenticated user to update their own `firstName`, `lastName`, `phoneNumber`, `imageUrl`, and optional `password`.
  - Guarded `if (userRequest.departmentId() != null)` in `UserServiceImpl.createUser` and `updateUser`.
