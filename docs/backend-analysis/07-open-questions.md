# Phase 7: Open Questions & Architectural Clarifications

This document outlines key technical and product questions that must be clarified with the backend and product teams before finalizing the Flutter mobile client implementation.

---

## 1. Authentication & User Onboarding

1. **User Registration Strategy**:
   - *Current Implementation*: No self-registration endpoint exists. Only an administrator can provision users via `POST /api/v1/users`.
   - *Question*: Is this an internal enterprise application where employee accounts are pre-provisioned by IT admins, or should public self-service registration (`POST /api/v1/auth/register`) be implemented for mobile users?
   - *If self-registration is needed*: Should newly registered users default to role `USER`? Is department selection required during registration, or can it be left null?

2. **Password Recovery & Reset**:
   - *Current Implementation*: No password reset or forgot password endpoints exist. Non-admin users cannot change their passwords.
   - *Question*: How should mobile users recover their accounts if they forget their password? Should we implement an email OTP (One-Time Password) or a magic reset link?
   - *Question*: Should an endpoint `PUT /api/v1/users/me/password` be added so users can change their own password from within the mobile app settings?

3. **Multi-Session & Token Invalidation**:
   - *Current Implementation*: When a user logs in, a new refresh token is saved to the `refresh_token` table. Older refresh tokens are not revoked upon new login.
   - *Question*: Is concurrent multi-device login supported, or should logging in on a second device invalidate previous refresh tokens?

---

## 2. Ticket Business Rules & Authorization Boundaries

1. **Ticket Modification Permissions**:
   - *Current Implementation*: `PUT /api/v1/tickets/{id}` permits any authenticated user (`ADMIN`, `USER`, `TECHNICIAN`) to update any non-completed ticket, regardless of who created or is assigned to it.
   - *Question*: Should authorization be restricted such that:
     - A `USER` can only update tickets they created?
     - A `TECHNICIAN` can only update tickets assigned to them?
     - Or can any user edit any open ticket?

2. **Ticket Deletion Policy**:
   - *Current Implementation*: `DELETE /api/v1/tickets/{id}` soft-deletes a ticket and is accessible to `ADMIN`, `USER`, and `TECHNICIAN`.
   - *Question*: Should regular users or technicians have permission to delete tickets, or should deletion be restricted strictly to `ADMIN`?

3. **Ticket Lifecycle & Reopening**:
   - *Current Implementation*: Status transitions are strictly one-way: `PENDING -> IN_PROGRESS -> COMPLETED`. Once `COMPLETED`, a ticket cannot be modified, deleted, or transitioned to any other status.
   - *Question*: Can a customer or technician reopen a completed ticket if the underlying issue recurs (e.g. `COMPLETED -> IN_PROGRESS`), or must a new ticket be created?

4. **Technician Assignment Workflow**:
   - *Current Implementation*: When a ticket transitions from `PENDING` to `IN_PROGRESS`, the backend automatically assigns the calling user as the technician if unassigned.
   - *Question*: Can a technician reassign a ticket to a colleague, or can only an `ADMIN` reassign tickets?

---

## 3. Real-Time Chat & Notification Delivery

1. **Push Notifications (FCM / APNS)**:
   - *Current Implementation*: No mobile push notification integration exists.
   - *Question*: When a user closes or backgrounds the Flutter mobile app, how should they be notified of ticket status changes or new chat messages? Should the backend integrate Firebase Cloud Messaging (FCM) to deliver push notifications?

2. **Native WebSocket STOMP Support**:
   - *Current Implementation*: [WebSocketConfig.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/config/WebSocketConfig.java) registers `/ws-chat` with `.withSockJS()`.
   - *Question*: Can the backend register a pure WebSocket STOMP endpoint (`registry.addEndpoint("/ws-chat")` without SockJS) to enable native WebSocket STOMP connections from Flutter packages like `stomp_dart_client`?

3. **WebSocket Security**:
   - *Current Implementation*: `/ws-chat/**` is configured as `permitAll()`.
   - *Question*: Can we add a STOMP `ChannelInterceptor` to authenticate client subscriptions via JWT Bearer token so users cannot listen to other tickets' chat channels?

---

## 4. API Standardization & Mobile Optimization

1. **Pagination for Chat & Users**:
   - *Current Implementation*: `GET /api/v1/tickets/{ticketId}/messages` and `GET /api/v1/users` return unpaginated lists.
   - *Question*: Can we introduce pagination query parameters (`page`, `size`) to chat messages and user listings before ticket history becomes too large for mobile memory?

2. **Date/Time Uniformity**:
   - *Current Implementation*: Timestamps alternate between `"yyyy-MM-dd HH:mm:ss"` and ISO-8601 strings without UTC offsets.
   - *Question*: Can the backend standardize all date/time fields to ISO-8601 UTC format (`yyyy-MM-dd'T'HH:mm:ss'Z'`) across all DTOs and entities?

3. **OpenAPI / Swagger Generation**:
   - *Current Implementation*: No OpenAPI spec generator is present.
   - *Question*: Can we add `springdoc-openapi-starter-webmvc-ui` to `pom.xml` so that the Flutter team can automatically generate Dart models and API client code?

4. **Image Upload Restrictions**:
   - *Current Implementation*: Multipart files up to 5MB are accepted with any file extension and forwarded to Supabase Storage.
   - *Question*: Should the backend enforce MIME type validation (e.g., `image/jpeg`, `image/png`, `image/webp`) to prevent non-image uploads?
