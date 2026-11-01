# Backend Feature Inventory (Source of Truth)

> **Backend Stack**: Spring Boot 4.0.3, Java 21, PostgreSQL (`helpdesk_db`), Spring Security + JWT (HS256), Spring Data JPA, WebSocket (STOMP).
> **Base Path**: `/api/v1` (REST) and `/ws-chat` (STOMP WebSocket).

---

## 1. Global Conventions & Security Model

### 1.1 Roles & Authorities (`com.zaby.helpdesk.enumeration.Role`)
- `USER` (`ROLE_USER`): Submits tickets, views own active/resolved tickets, chats on tickets, views own profile & activity feed.
- `TECHNICIAN` (`ROLE_TECHNICIAN`): Views organization ticket queue, accepts (`IN_PROGRESS`) and resolves (`COMPLETED`) tickets, manages categories/subcategories, views dashboard stats, chats on tickets.
- `ADMIN` (`ROLE_ADMIN`): Full system access including all technician capabilities plus User CRUD and Department CRUD.

### 1.2 Enums
- **Status (`Status`)**: `PENDING` -> `IN_PROGRESS` -> `COMPLETED` (strict forward-only transition; `COMPLETED` tickets are immutable and cannot be edited or deleted).
- **Priority (`Priority`)**: `LOW`, `MEDIUM`, `HIGH`.
- **Audit Action (`Action`)**: `CREATE`, `UPDATE`, `DELETE`, `ASSIGN`, `COMMENT`.

### 1.3 Error Response Format (`GlobalExceptionHandler`)
```json
{
  "timestamp": "2026-11-01T22:45:00",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Validation failed",
  "fieldErrors": [
    { "field": "ticketTitle", "message": "must not be blank" }
  ]
}
```

### 1.4 Pagination Convention
Spring Data `Page<T>` query parameters:
- `page` (int, default `0`)
- `size` (int, default `10`)

---

## 2. Feature Inventory by Module

### Module 1: Authentication & Session Management (`AuthController`)

| # | Feature Name | Method & Path | Request Body / Params | Response Shape | Permitted Roles | Status |
|---|---|---|---|---|---|---|
| **1.1** | **User Login** | `POST /api/v1/auth/login` | `LoginRequest`: `{ email: @Email @NotBlank, password: @NotBlank }` | `AuthResponse`: `{ id, email, message, firstName, lastName, imageUrl, departmentId, role, accessToken, refreshToken }` + `HttpOnly` cookie `refreshToken` | Public (`permitAll`) | **Working** |
| **1.2** | **Refresh Access Token** | `POST /api/v1/auth/refresh` | `RefreshTokenRequest`: `{ refreshToken }` (or `refreshToken` cookie) | `AuthResponse` with new `accessToken` | Public (`permitAll`) | **Working** |
| **1.3** | **Logout & Revoke Token** | `POST /api/v1/auth/logout` | `RefreshTokenRequest`: `{ refreshToken }` (or `refreshToken` cookie) | `204 No Content` (clears DB token + cookie) | Public (`permitAll`) | **Working** |

---

### Module 2: User Management & Profile (`UserController`)

| # | Feature Name | Method & Path | Request Body / Params | Response Shape | Permitted Roles | Status |
|---|---|---|---|---|---|---|
| **2.1** | **Get Current User Profile (`/me`)** | `GET /api/v1/users/me` | None (uses JWT principal) | `UserResponse`: `{ id, firstName, lastName, email, phoneNumber, imageUrl, role, departmentId, departmentName, createdAt, updatedAt, ticketsCreated, ticketsAssigned, ticketsResolved, ticketsOpen }` | Authenticated (`USER`, `TECHNICIAN`, `ADMIN`) | **Working** |
| **2.2** | **Update Current User Profile** | *(Missing self-update endpoint; `PUT /api/v1/users/{id}` is restricted to `ADMIN`)* | `UserRequest` | `UserResponse` | Currently `ADMIN` only | **Incomplete** (Non-admin users cannot save profile/settings changes without `PUT /api/v1/users/me`) |
| **2.3** | **Create User** | `POST /api/v1/users` | `UserRequest`: `{ firstName, lastName, email, phoneNumber, password, imageUrl, role, departmentId }` | `201 Created` -> `UserResponse` | `ADMIN` | **Working** (Bug if `departmentId` is null) |
| **2.4** | **Get All Users** | `GET /api/v1/users` | Optional `?page=0&size=10` | `List<UserResponse>` (if no page param) or `Page<UserResponse>` | `ADMIN` | **Working** |
| **2.5** | **Get User By ID** | `GET /api/v1/users/{id}` | Path `{id}` | `UserResponse` (with ticket metrics) | `ADMIN` | **Working** |
| **2.6** | **Update User** | `PUT /api/v1/users/{id}` | Path `{id}`, Body: `UserRequest` | `UserResponse` | `ADMIN` | **Working** |
| **2.7** | **Soft-Delete User** | `DELETE /api/v1/users/{id}` | Path `{id}` | `UserResponse` (`deleted = true`) | `ADMIN` | **Working** |

---

### Module 3: Ticket Lifecycle & Operations (`TicketController`)

| # | Feature Name | Method & Path | Request Body / Params | Response Shape | Permitted Roles | Status |
|---|---|---|---|---|---|---|
| **3.1** | **Create Ticket** | `POST /api/v1/tickets` | `TicketRequest`: `{ ticketTitle: @NotBlank, description: @NotBlank, priority: @NotNull, categoryId, subCategoryId, assignedTo, imageUrls: List<String> }` | `201 Created` -> `TicketResponse` | `USER`, `TECHNICIAN`, `ADMIN` | **Working** |
| **3.2** | **Get Ticket By ID** | `GET /api/v1/tickets/{id}` | Path `{id}` | `TicketResponse`: `{ id, ticketTitle, description, priority, status, categoryId, categoryName, subCategoryId, subCategoryName, assignedTo, assignedName, logs, createdBy, createdByName, createdAt, completedAt, imageUrls }` | `USER`, `TECHNICIAN`, `ADMIN` | **Working** |
| **3.3** | **Get Active Tickets (Org Queue)** | `GET /api/v1/tickets` | `?page=0&size=10&status=&priority=&search=` | `Page<TicketResponse>` (default statuses: `PENDING`, `IN_PROGRESS`) | `USER`, `TECHNICIAN`, `ADMIN` | **Working** |
| **3.4** | **Get My Active Tickets** | `GET /api/v1/tickets/my` | `?page=0&size=10&status=&priority=&search=` | `Page<TicketResponse>` (filtered by `createdBy = currentUserId`, non-completed) | `USER`, `TECHNICIAN`, `ADMIN` | **Working** |
| **3.5** | **Get Completed Ticket History** | `GET /api/v1/tickets/history` | `?page=0&size=10&status=&priority=&search=` | `Page<TicketResponse>` (`COMPLETED` tickets; `USER` sees own created, `TECHNICIAN` sees own assigned, `ADMIN` sees all) | `USER`, `TECHNICIAN`, `ADMIN` | **Working** |
| **3.6** | **Update Ticket Status (Accept / Close)** | `PATCH /api/v1/tickets/{id}/status` | Path `{id}`, Body `UpdateStatusRequest`: `{ status: @NotNull, remark }` | `TicketResponse` (auto-assigns technician when transitioning `PENDING -> IN_PROGRESS`) | `USER`, `TECHNICIAN`, `ADMIN` | **Working** |
| **3.7** | **Update Ticket Details** | `PUT /api/v1/tickets/{id}` | Path `{id}`, Body `TicketRequest` | `TicketResponse` | `USER`, `TECHNICIAN`, `ADMIN` | **Buggy** (`TicketServiceImpl.updateTicket` does not persist updated `imageUrls`) |
| **3.8** | **Soft-Delete Ticket** | `DELETE /api/v1/tickets/{id}` | Path `{id}` | `204 No Content` (blocks deleting `COMPLETED` tickets) | `USER`, `TECHNICIAN`, `ADMIN` | **Working** |

---

### Module 4: Real-Time & REST Ticket Messaging (`ChatController`)

| # | Feature Name | Method & Path | Request Body / Params | Response Shape | Permitted Roles | Status |
|---|---|---|---|---|---|---|
| **4.1** | **Get Ticket Chat Messages** | `GET /api/v1/tickets/{ticketId}/messages` | Path `{ticketId}`, Optional `?page=&size=` | `List<ChatMessage>` or `Page<ChatMessage>`: `{ id, ticketId, senderId, sender, content, timestamp }` | Authenticated | **Working** |
| **4.2** | **Send Chat Message (STOMP)** | STOMP `/app/chat` -> broadcasts `/topic/ticket/{ticketId}` | `ChatMessage`: `{ ticketId, senderId, sender, content }` | Broadcasts saved `ChatMessage` | Authenticated | **Working** (Lacks REST `POST /api/v1/tickets/{ticketId}/messages` fallback for mobile HTTP clients) |

---

### Module 5: Incident Categories & Sub-Categories (`CategoryController`)

| # | Feature Name | Method & Path | Request Body / Params | Response Shape | Permitted Roles | Status |
|---|---|---|---|---|---|---|
| **5.1** | **Get All Root Categories (with Sub-Categories)** | `GET /api/v1/categories` | None | `List<CategoryResponse>`: `{ id, name, description, parentId, subCategories: List<CategoryResponse> }` | Authenticated | **Working** |
| **5.2** | **Get Category By ID** | `GET /api/v1/categories/{id}` | Path `{id}` | `CategoryResponse` | Authenticated | **Unused** |
| **5.3** | **Create Category / Sub-Category** | `POST /api/v1/categories` | `CategoryRequest`: `{ name: @NotBlank, description, parentId }` | `201 Created` -> `CategoryResponse` | `ADMIN`, `TECHNICIAN` | **Working** |
| **5.4** | **Update Category / Sub-Category** | `PUT /api/v1/categories/{id}` | Path `{id}`, Body `CategoryRequest` | `CategoryResponse` | `ADMIN`, `TECHNICIAN` | **Working** (Unused by clients) |
| **5.5** | **Delete Category / Sub-Category** | `DELETE /api/v1/categories/{id}` | Path `{id}` | `204 No Content` (fails with `400` if used by any tickets) | `ADMIN`, `TECHNICIAN` | **Working** |

---

### Module 6: Departments (`DepartmentController`)

| # | Feature Name | Method & Path | Request Body / Params | Response Shape | Permitted Roles | Status |
|---|---|---|---|---|---|---|
| **6.1** | **Get All Departments** | `GET /api/v1/departments` | None | `List<DepartmentResponse>`: `{ id, name, description }` | Authenticated | **Working** |
| **6.2** | **Get Department By ID** | `GET /api/v1/departments/{id}` | Path `{id}` | `DepartmentResponse` | Authenticated | **Unused** |
| **6.3** | **Create Department** | `POST /api/v1/departments` | `DepartmentRequest`: `{ name: @NotBlank, description }` | `201 Created` -> `DepartmentResponse` | `ADMIN` | **Working** |
| **6.4** | **Update Department** | `PUT /api/v1/departments/{id}` | Path `{id}`, Body `DepartmentRequest` | `DepartmentResponse` | `ADMIN` | **Working** (Unused by clients) |

---

### Module 7: File & Image Attachment Upload (`ImageController`)

| # | Feature Name | Method & Path | Request Body / Params | Response Shape | Permitted Roles | Status |
|---|---|---|---|---|---|---|
| **7.1** | **Upload Image Attachment** | `POST /api/v1/images/upload` | `multipart/form-data`: `file` (max 5MB) | `{ "url": "https://..." }` | Authenticated | **Working** |

---

### Module 8: Dashboard Analytics & Activity Feed (`TicketController`)

| # | Feature Name | Method & Path | Request Body / Params | Response Shape | Permitted Roles | Status |
|---|---|---|---|---|---|---|
| **8.1** | **Dashboard KPI Statistics** | `GET /api/v1/tickets/stats` | None | `DashboardStatsResponse`: `{ totalTickets, pendingTickets, inProgressTickets, completedTickets, totalUsers }` | `USER`, `TECHNICIAN`, `ADMIN` | **Working** |
| **8.2** | **Monthly Received vs Due Chart** | `GET /api/v1/tickets/stats/monthly` | Optional `?year=2026` | `MonthlyChartResponse`: `{ received: List<Long>[12], due: List<Long>[12] }` | `USER`, `TECHNICIAN`, `ADMIN` | **Working** |
| **8.3** | **Role-Filtered Activity Feed (Notifications)** | `GET /api/v1/tickets/activity` | `?page=0&size=10` | `Page<NotificationResponse>`: `{ id, image, title, subTitle, createdAt }` | `USER`, `TECHNICIAN`, `ADMIN` | **Working** |

---

### Module 9: Audit Logs & Status Timelines (`TicketHistoryController`, `TicketStatusLogController`)

| # | Feature Name | Method & Path | Request Body / Params | Response Shape | Permitted Roles | Status |
|---|---|---|---|---|---|---|
| **9.1** | **Get Ticket Audit History** | `GET /api/v1/history/ticket/{ticketId}` | Path `{ticketId}` | `List<TicketHistoryResponse>`: `{ id, ticketId, ticketTitle, action, comment, performById, performByName, performAt }` | Authenticated | **Working** (Unused by clients) |
| **9.2** | **Get Ticket Status Transition Logs** | `GET /api/v1/status_log/ticket/{ticketId}` | Path `{ticketId}` | `List<TicketStatusLogResponse>`: `{ id, ticketId, fromStatus, toStatus, changedById, changedByName, remark, changedAt }` | Authenticated | **Working** (Embedded in `TicketResponse.logs`, standalone endpoint unused) |

---

### Module 10: Push Notification Device Tokens (`NotificationTokenController`)

| # | Feature Name | Method & Path | Request Body / Params | Response Shape | Permitted Roles | Status |
|---|---|---|---|---|---|---|
| **10.1** | **Register Device Token** | `POST /api/v1/notifications/device-token` | `DeviceTokenRequest`: `{ deviceToken: @NotBlank, deviceType: @NotBlank }` | `{ id, deviceToken, deviceType, message }` | Authenticated | **Working** (Unused by mobile client) |
| **10.2** | **Unregister Device Token** | `DELETE /api/v1/notifications/device-token` | `?deviceToken=...` | `204 No Content` | Authenticated | **Working** (Unused by mobile client) |
