# Phase 3: Comprehensive Endpoint Inventory

This document provides an exhaustive reference for all 37 endpoints (36 REST HTTP endpoints + 1 WebSocket STOMP endpoint) implemented in the Spring Boot backend. Every endpoint includes method, route, controller citation, required roles, request schemas, validation rules, response schemas, status codes, and error cases.

---

## Table of Contents
1. [Authentication Module (`/api/v1/auth`)](#1-authentication-module-apiv1auth)
2. [Tickets Module (`/api/v1/tickets`)](#2-tickets-module-apiv1tickets)
3. [Ticket Status Log Module (`/api/v1/status_log`)](#3-ticket-status-log-module-apiv1status_log)
4. [Ticket History Module (`/api/v1/history`)](#4-ticket-history-module-apiv1history)
5. [Categories Module (`/api/v1/categories`)](#5-categories-module-apiv1categories)
6. [Departments Module (`/api/v1/departments`)](#6-departments-module-apiv1departments)
7. [Users Module (`/api/v1/users`)](#7-users-module-apiv1users)
8. [Image Management Module (`/api/v1/images`)](#8-image-management-module-apiv1images)
9. [Real-Time Chat & Messaging Module](#9-real-time-chat--messaging-module)
10. [Debug Endpoints (`/api/v1/debug`)](#10-debug-endpoints-apiv1debug)

---

## 1. Authentication Module (`/api/v1/auth`)

### 1.1 POST `/api/v1/auth/login`
- **Controller**: [AuthController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/AuthController.java#L24) (`login`)
- **Service**: [AuthServiceImpl.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/service/impl/AuthServiceImpl.java#L26) (`login`)
- **Security / Roles**: Public (`permitAll`). No token required.
- **Request Headers**: `Content-Type: application/json`
- **Request Body** ([LoginRequest.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/request/LoginRequest.java)):
  | Field | Type | Required | Validation Rules | Description |
  | :--- | :--- | :--- | :--- | :--- |
  | `email` | String | Yes | None in record | User email address |
  | `password` | String | Yes | None in record | Raw account password |
- **Response Body** ([AuthResponse.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/response/AuthResponse.java)):
  | Field | Type | Description |
  | :--- | :--- | :--- |
  | `email` | String | User's registered email |
  | `message` | String | Status message (`"Login successful"`) |
  | `firstName` | String | User first name |
  | `lastName` | String | User last name |
  | `departmentId`| Long (nullable)| Assigned department ID |
  | `role` | String (Enum) | `ADMIN`, `USER`, or `TECHNICIAN` |
  | `accessToken` | String | JWT access token (valid for 15 minutes) |
  | `refreshToken`| String | UUID token (valid for 7 days) |
- **Status Codes**:
  - `200 OK`: Login successful. Sets `Set-Cookie: refreshToken=...; HttpOnly`.
  - `401 UNAUTHORIZED`: Bad credentials (handled by `GlobalExceptionHandler`).
  - `500 INTERNAL_SERVER_ERROR`: Unhandled error.

### 1.2 POST `/api/v1/auth/refresh`
- **Controller**: [AuthController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/AuthController.java#L30) (`refresh`)
- **Security / Roles**: Public (`permitAll`).
- **Request Headers**: `Content-Type: application/json`
- **Request Body** ([RefreshTokenRequest.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/request/RefreshTokenRequest.java)):
  | Field | Type | Required | Description |
  | :--- | :--- | :--- | :--- |
  | `refreshToken` | String | Yes | Valid UUID refresh token string |
- **Response Body**: Identical to `AuthResponse` (`"Token refreshed"`, new `accessToken`, same `refreshToken`).
- **Status Codes**:
  - `200 OK`: Successfully refreshed.
  - `401 UNAUTHORIZED`: Refresh token missing, expired, or not found in database.
  - `500 INTERNAL_SERVER_ERROR`: Thrown if user has null department due to unhandled NPE.

### 1.3 POST `/api/v1/auth/logout`
- **Controller**: [AuthController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/AuthController.java#L59) (`logout`)
- **Security / Roles**: Public (`permitAll`).
- **Request Body** ([RefreshTokenRequest.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/request/RefreshTokenRequest.java)):
  | Field | Type | Required | Description |
  | :--- | :--- | :--- | :--- |
  | `refreshToken` | String | Optional | Refresh token to invalidate from DB |
- **Response Body**: Empty.
- **Status Codes**:
  - `200 OK`: Logout processed and DB token revoked.

---

## 2. Tickets Module (`/api/v1/tickets`)

### 2.1 POST `/api/v1/tickets`
- **Controller**: [TicketController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketController.java#L23) (`createTicket`)
- **Service**: [TicketServiceImpl.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/service/impl/TicketServiceImpl.java#L87) (`createTicket`)
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`
- **Request Body** ([TicketRequest.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/request/TicketRequest.java)):
  | Field | Type | Required | Validation Rules | Description |
  | :--- | :--- | :--- | :--- | :--- |
  | `ticketTitle` | String | Yes | `@NotBlank` in DTO | Title of the incident/ticket |
  | `description` | String | Yes | `@NotBlank` in DTO | Detailed description |
  | `priority` | String (Enum) | Yes | `@NotNull`, values: `HIGH`, `MEDIUM`, `LOW` | Severity level |
  | `categoryId` | Long | Yes (Runtime)| Must reference existing category | ID of parent category |
  | `subCategoryId`| Long | Optional | Must reference existing subcategory | ID of subcategory |
  | `assignedTo` | Long | Optional | Must reference existing user | Assignee user ID |
  | `imageUrls` | List<String> | Optional | Valid URLs from image upload endpoint | List of attached Supabase image URLs |
- **Side Effects**: Automatically sets status to `PENDING`, sets `createdBy` to current authenticated user ID, inserts initial record in `ticket_status_log` (`"Ticket Created"`), and inserts initial entry in `ticket_history` (`Action.CREATE`).
- **Response Body** ([TicketResponse.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/response/TicketResponse.java)): Complete ticket model.
- **Status Codes**:
  - `201 CREATED`: Ticket successfully created.
  - `401 UNAUTHORIZED`: Missing/invalid Bearer token.
  - `500 INTERNAL_SERVER_ERROR`: If categoryId is invalid (`RuntimeException` thrown).

### 2.2 GET `/api/v1/tickets/{id}`
- **Controller**: [TicketController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketController.java#L30) (`getById`)
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`
- **Path Parameters**: `id` (Long) - Ticket identifier
- **Response Body**: [TicketResponse.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/response/TicketResponse.java):
  ```json
  {
    "id": 1,
    "ticketTitle": "Network printer offline",
    "description": "Floor 2 printer is unresponsive",
    "priority": "HIGH",
    "status": "PENDING",
    "categoryId": 2,
    "categoryName": "Hardware",
    "subCategoryId": 5,
    "subCategoryName": "Printers",
    "assignedTo": 3,
    "assignedName": "Tech One",
    "logs": [ ... ],
    "createdBy": 12,
    "createdByName": "John Doe",
    "createdAt": "2026-10-22 10:15:00",
    "completedAt": null,
    "imageUrls": [ "https://...supabase.co/storage/v1/object/public/Image/..." ]
  }
  ```
- **Status Codes**:
  - `200 OK`: Ticket found and returned.
  - `401 UNAUTHORIZED`: Authentication missing.
  - `500 INTERNAL_SERVER_ERROR`: Ticket not found or marked soft-deleted (`RuntimeException("Ticket Not Found")`).

### 2.3 GET `/api/v1/tickets`
- **Controller**: [TicketController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketController.java#L37) (`getAll`)
- **Description**: Returns all non-deleted tickets whose status is either `PENDING` or `IN_PROGRESS`.
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`
- **Query Parameters**:
  | Param | Type | Default | Description |
  | :--- | :--- | :--- | :--- |
  | `page` | int | `0` | Zero-based page index |
  | `size` | int | `10` | Page size |
- **Response Body**: Spring Data `Page<TicketResponse>`.
  ```json
  {
    "content": [ { ...TicketResponse... } ],
    "pageable": { "pageNumber": 0, "pageSize": 10, ... },
    "totalPages": 3,
    "totalElements": 25,
    "last": false,
    "size": 10,
    "number": 0
  }
  ```
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`.

### 2.4 GET `/api/v1/tickets/history`
- **Controller**: [TicketController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketController.java#L44) (`getHistory`)
- **Description**: Returns completed tickets (`status = COMPLETED`), filtered dynamically by role:
  - `ROLE_USER`: Only completed tickets created by the current user.
  - `ROLE_TECHNICIAN`: Only completed tickets assigned to the current technician.
  - `ROLE_ADMIN`: All completed tickets across the organization.
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`
- **Query Parameters**: `page` (default: 0), `size` (default: 10).
- **Response Body**: `Page<TicketResponse>`.
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`.

### 2.5 GET `/api/v1/tickets/my`
- **Controller**: [TicketController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketController.java#L56) (`getMyTickets`)
- **Description**: Returns active tickets (`PENDING` or `IN_PROGRESS`) created by the authenticated user (`createdBy == currentUserId`).
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`
- **Query Parameters**: `page` (default: 0), `size` (default: 10).
- **Response Body**: `Page<TicketResponse>`.
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`.

### 2.6 PATCH `/api/v1/tickets/{id}/status`
- **Controller**: [TicketController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketController.java#L67) (`updateTicketStatus`)
- **Service**: [TicketServiceImpl.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/service/impl/TicketServiceImpl.java#L195)
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`
- **Path Parameters**: `id` (Long) - Ticket ID
- **Request Body** ([UpdateStatusRequest.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/request/UpdateStatusRequest.java)):
  ```json
  {
    "status": "IN_PROGRESS",
    "remark": "Technician accepted the ticket"
  }
  ```
- **Business Logic & Transitions**:
  - `PENDING` -> can only transition to `IN_PROGRESS`.
  - `IN_PROGRESS` -> can only transition to `COMPLETED`.
  - `COMPLETED` -> cannot transition anywhere.
  - **Auto-assignment**: If transitioning to `IN_PROGRESS` and `assignedTo` is null, the current user is automatically assigned as technician!
  - Creates a new `TicketStatusLog` and a new `TicketHistory` entry.
- **Status Codes**:
  - `200 OK`: Status updated.
  - `401 UNAUTHORIZED`: Authentication missing.
  - `500 INTERNAL_SERVER_ERROR`: If invalid transition attempted (`IllegalArgumentException`), ticket is completed, or ticket is deleted.

### 2.7 PUT `/api/v1/tickets/{id}`
- **Controller**: [TicketController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketController.java#L78) (`updateTicket`)
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`
- **Path Parameters**: `id` (Long)
- **Request Body**: [TicketRequest.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/request/TicketRequest.java)
- **Constraints**: Completed tickets cannot be modified. Deleted tickets cannot be updated.
- **Response Body**: `TicketResponse`.
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`, `500 INTERNAL_SERVER_ERROR`.

### 2.8 DELETE `/api/v1/tickets/{id}`
- **Controller**: [TicketController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketController.java#L85) (`deleteTicket`)
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`
- **Path Parameters**: `id` (Long)
- **Behavior**: Performs **soft delete** (`ticket.setDeleted(true)`). Adds `Action.DELETE` to ticket history.
- **Constraints**: Completed tickets cannot be deleted (`IllegalArgumentException`).
- **Response Body**: Empty.
- **Status Codes**:
  - `204 NO CONTENT`: Deleted successfully.
  - `401 UNAUTHORIZED`: Not authenticated.
  - `500 INTERNAL_SERVER_ERROR`: Completed ticket or ticket not found.

### 2.9 GET `/api/v1/tickets/stats`
- **Controller**: [TicketController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketController.java#L94) (`getStats`)
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`
- **Response Body** ([DashboardStatsResponse.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/response/DashboardStatsResponse.java)):
  ```json
  {
    "totalTickets": 42,
    "pendingTickets": 10,
    "inProgressTickets": 7,
    "completedTickets": 25,
    "totalUsers": 14
  }
  ```
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`.

### 2.10 GET `/api/v1/tickets/stats/monthly`
- **Controller**: [TicketController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketController.java#L100) (`getMonthlyStats`)
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`
- **Query Parameters**: `year` (Integer, optional - defaults to current calendar year)
- **Response Body** ([MonthlyChartResponse.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/response/MonthlyChartResponse.java)):
  ```json
  {
    "received": [2, 4, 1, 0, 5, 3, 2, 8, 4, 1, 0, 0],
    "due": [1, 2, 0, 0, 1, 1, 0, 2, 1, 0, 0, 0]
  }
  ```
  *(12-element arrays corresponding to Jan–Dec; `received` represents COMPLETED tickets, `due` represents PENDING + IN_PROGRESS).*
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`.

### 2.11 GET `/api/v1/tickets/activity`
- **Controller**: [TicketController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketController.java#L109) (`getActivityFeed`)
- **Description**: Activity notification stream derived from `ticket_history`. Filtered by user role:
  - `ROLE_USER`: Activities on tickets created by the user.
  - `ROLE_TECHNICIAN`: Activities on tickets assigned to the technician.
  - `ROLE_ADMIN`: All ticket activities.
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`
- **Query Parameters**: `page` (default: 0), `size` (default: 10).
- **Response Body**: `Page<NotificationResponse>`:
  ```json
  {
    "content": [
      {
        "id": 101,
        "image": "/images/user/user-01.png",
        "title": "Ticket UPDATE",
        "subTitle": "Ticket #1: Network printer offline",
        "createdAt": "2026-10-22T11:20:30"
      }
    ],
    "totalPages": 5,
    "totalElements": 48
  }
  ```
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`.

---

## 3. Ticket Status Log Module (`/api/v1/status_log`)

### 3.1 GET `/api/v1/status_log/ticket/{ticketId}`
- **Controller**: [TicketStatusLogController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketStatusLogController.java#L22) (`getLogsByTicket`)
- **Security / Roles**: Any authenticated user (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`)
- **Path Parameters**: `ticketId` (Long)
- **Response Body**: `List<TicketStatusLogResponse>`:
  ```json
  [
    {
      "id": 1,
      "ticketId": 1,
      "fromStatus": null,
      "toStatus": "PENDING",
      "changedById": 12,
      "changedByName": "John Doe",
      "remark": "Ticket Created",
      "changedAt": "2026-10-22T10:15:00"
    },
    {
      "id": 2,
      "ticketId": 1,
      "fromStatus": "PENDING",
      "toStatus": "IN_PROGRESS",
      "changedById": 3,
      "changedByName": "Tech One",
      "remark": "Assigned and working",
      "changedAt": "2026-10-22T10:30:00"
    }
  ]
  ```
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`.

---

## 4. Ticket History Module (`/api/v1/history`)

### 4.1 GET `/api/v1/history/ticket/{ticketId}`
- **Controller**: [TicketHistoryController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/TicketHistoryController.java#L21) (`getHistoryByTicket`)
- **Security / Roles**: Any authenticated user (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`)
- **Path Parameters**: `ticketId` (Long)
- **Response Body**: `List<TicketHistoryResponse>`:
  ```json
  [
    {
      "id": 1,
      "ticketId": 1,
      "ticketTitle": "Network printer offline",
      "action": "CREATE",
      "comment": "Ticket Created",
      "performById": 12,
      "performByName": "John Doe",
      "performAt": "2026-10-22T10:15:00"
    }
  ]
  ```
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`.

---

## 5. Categories Module (`/api/v1/categories`)

### 5.1 POST `/api/v1/categories`
- **Controller**: [CategoryController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/CategoryController.java#L21) (`createCategory`)
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_TECHNICIAN`
- **Request Body** ([CategoryRequest.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/request/CategoryRequest.java)):
  | Field | Type | Required | Description |
  | :--- | :--- | :--- | :--- |
  | `name` | String | Yes | Category title |
  | `description` | String | Optional | Category details |
  | `parentId` | Long | Optional | Parent category ID (null if top-level) |
- **Response Body**: `CategoryResponse`.
- **Status Codes**:
  - `201 CREATED`: Created successfully.
  - `401 UNAUTHORIZED`: Authentication missing.
  - `403 FORBIDDEN`: Regular `ROLE_USER` lacks permission.
  - `404 NOT_FOUND`: If `parentId` does not exist (`ResourceNotFoundException`).

### 5.2 PUT `/api/v1/categories/{id}`
- **Controller**: [CategoryController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/CategoryController.java#L29) (`updateCategory`)
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_TECHNICIAN`
- **Path Parameters**: `id` (Long)
- **Request Body**: `CategoryRequest`.
- **Validation**: Circular parent check (`id.equals(parentId)` throws `BusinessException`).
- **Status Codes**: `200 OK`, `400 BAD_REQUEST`, `403 FORBIDDEN`, `404 NOT_FOUND`.

### 5.3 GET `/api/v1/categories`
- **Controller**: [CategoryController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/CategoryController.java#L37) (`getAllCategories`)
- **Description**: Returns all root-level categories (`parent IS NULL`). Subcategories are nested inside `subCategories`.
- **Security / Roles**: Authenticated (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`).
- **Response Body**: `List<CategoryResponse>`:
  ```json
  [
    {
      "id": 1,
      "name": "Hardware",
      "description": "Physical equipment issues",
      "parentId": null,
      "subCategories": [
        {
          "id": 4,
          "name": "Laptops",
          "description": "MacBook and ThinkPad issues",
          "parentId": 1,
          "subCategories": []
        }
      ]
    }
  ]
  ```
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`.

### 5.4 GET `/api/v1/categories/{id}`
- **Controller**: [CategoryController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/CategoryController.java#L44) (`getCategoryById`)
- **Security / Roles**: Authenticated.
- **Path Parameters**: `id` (Long).
- **Status Codes**: `200 OK`, `404 NOT_FOUND`.

### 5.5 DELETE `/api/v1/categories/{id}`
- **Controller**: [CategoryController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/CategoryController.java#L51) (`deleteCategory`)
- **Security / Roles**: `ROLE_ADMIN`, `ROLE_TECHNICIAN`.
- **Path Parameters**: `id` (Long).
- **Validation**: Checks if any tickets are associated with category or subcategory; throws `BusinessException` if `ticketsCount > 0`.
- **Status Codes**:
  - `204 NO CONTENT`: Deleted.
  - `400 BAD_REQUEST`: Category is referenced by existing tickets.
  - `403 FORBIDDEN`: Role USER attempted deletion.
  - `404 NOT_FOUND`: Category does not exist.

---

## 6. Departments Module (`/api/v1/departments`)

### 6.1 POST `/api/v1/departments`
- **Controller**: [DepartmentController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/DepartmentController.java#L20) (`createDepartment`)
- **Security / Roles**: `ROLE_ADMIN` only.
- **Request Body** ([DepartmentRequest.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/request/DepartmentRequest.java)):
  | Field | Type | Required | Description |
  | :--- | :--- | :--- | :--- |
  | `name` | String | Yes | Department title (e.g., `"IT"`, `"HR"`) |
  | `description` | String | Optional | Description |
- **Response Body**: [DepartmentResponse.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/response/DepartmentResponse.java).
- **Status Codes**: `201 CREATED`, `401 UNAUTHORIZED`, `403 FORBIDDEN`.

### 6.2 PUT `/api/v1/departments/{id}`
- **Controller**: [DepartmentController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/DepartmentController.java#L27) (`updateDepartment`)
- **Security / Roles**: `ROLE_ADMIN` only.
- **Path Parameters**: `id` (Long).
- **Request Body**: `DepartmentRequest`.
- **Status Codes**: `200 OK`, `403 FORBIDDEN`, `500 INTERNAL_SERVER_ERROR` (throws `RuntimeException` on not found).

### 6.3 GET `/api/v1/departments/{id}`
- **Controller**: [DepartmentController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/DepartmentController.java#L34) (`getDepartmentById`)
- **Security / Roles**: Authenticated (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`).
- **Path Parameters**: `id` (Long).
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`, `500 INTERNAL_SERVER_ERROR` (if ID not found).

### 6.4 GET `/api/v1/departments`
- **Controller**: [DepartmentController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/DepartmentController.java#L41) (`getAllDepartments`)
- **Security / Roles**: Authenticated.
- **Response Body**: `List<DepartmentResponse>`:
  ```json
  [
    { "id": 1, "name": "IT", "description": "Information Technology" },
    { "id": 2, "name": "HR", "description": "Human Resources" },
    { "id": 3, "name": "Finance", "description": "Finance & Accounting" }
  ]
  ```
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`.

*(Note: There is NO DELETE endpoint for departments).*

---

## 7. Users Module (`/api/v1/users`)

### 7.1 POST `/api/v1/users`
- **Controller**: [UserController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/UserController.java#L24) (`createUser`)
- **Security / Roles**: `ROLE_ADMIN` only.
- **Request Body** ([UserRequest.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/request/UserRequest.java)):
  | Field | Type | Required | Description |
  | :--- | :--- | :--- | :--- |
  | `firstName` | String | Yes | First name |
  | `lastName` | String | Yes | Last name |
  | `email` | String | Yes | Unique email address |
  | `phoneNumber`| String | Optional | Contact phone number |
  | `password` | String | Yes | Plaintext password (will be BCrypt encoded) |
  | `imageUrl` | String | Optional | Profile image URL |
  | `role` | String (Enum) | Yes | `ADMIN`, `USER`, or `TECHNICIAN` |
  | `departmentId`| Long | Yes | Department ID to link user |
- **Status Codes**:
  - `201 CREATED`: User created.
  - `401 UNAUTHORIZED`, `403 FORBIDDEN`.
  - `500 INTERNAL_SERVER_ERROR`: If email already exists or department not found (`RuntimeException`).

### 7.2 GET `/api/v1/users/me`
- **Controller**: [UserController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/UserController.java#L30) (`getCurrentUser`)
- **Description**: Returns full user profile for the authenticated caller with computed ticket metrics.
- **Security / Roles**: Authenticated (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`).
- **Response Body** ([UserResponse.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/response/UserResponse.java)):
  ```json
  {
    "id": 12,
    "firstName": "John",
    "lastName": "Doe",
    "email": "user@example.com",
    "phoneNumber": "+123456789",
    "imageUrl": "https://.../avatar.png",
    "role": "USER",
    "departmentId": 1,
    "departmentName": "IT",
    "createdAt": "2026-10-22T08:00:00",
    "updatedAt": "2026-10-22T08:00:00",
    "ticketsCreated": 5,
    "ticketsAssigned": 0,
    "ticketsResolved": 3,
    "ticketsOpen": 2
  }
  ```
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`.

### 7.3 GET `/api/v1/users/{id}`
- **Controller**: [UserController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/UserController.java#L42) (`getUser`)
- **Security / Roles**: `ROLE_ADMIN` only.
- **Path Parameters**: `id` (Long).
- **Response Body**: `UserResponse` with computed ticket metrics.
- **Status Codes**: `200 OK`, `403 FORBIDDEN`, `500 INTERNAL_SERVER_ERROR`.

### 7.4 GET `/api/v1/users`
- **Controller**: [UserController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/UserController.java#L49) (`getAllUsers`)
- **Description**: Returns all non-deleted users.
- **Security / Roles**: `ROLE_ADMIN` only.
- **Response Body**: `List<UserResponse>`.
- **Status Codes**: `200 OK`, `403 FORBIDDEN`.

### 7.5 PUT `/api/v1/users/{id}`
- **Controller**: [UserController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/UserController.java#L56) (`updateUser`)
- **Security / Roles**: `ROLE_ADMIN` only.
- **Path Parameters**: `id` (Long).
- **Request Body**: `UserRequest`.
- **Status Codes**: `200 OK`, `403 FORBIDDEN`, `500 INTERNAL_SERVER_ERROR`.

### 7.6 DELETE `/api/v1/users/{id}`
- **Controller**: [UserController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/UserController.java#L63) (`deleteUser`)
- **Security / Roles**: `ROLE_ADMIN` only.
- **Path Parameters**: `id` (Long).
- **Behavior**: Performs **soft delete** (`user.setDeleted(true)`). Returns `200 OK` with the deleted `UserResponse` (NOT 204!).
- **Status Codes**: `200 OK`, `403 FORBIDDEN`, `500 INTERNAL_SERVER_ERROR`.

---

## 8. Image Management Module (`/api/v1/images`)

### 8.1 POST `/api/v1/images/upload`
- **Controller**: [ImageController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/ImageController.java#L21) (`uploadImage`)
- **Service**: [SupabaseStorageService.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/service/SupabaseStorageService.java#L31) (`uploadFile`)
- **Security / Roles**: Authenticated (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`).
- **Request Headers**: `Content-Type: multipart/form-data`
- **Request Form Part**:
  | Part Name | Type | Limit | Description |
  | :--- | :--- | :--- | :--- |
  | `file` | MultipartFile | 5 MB | Image file binary (JPEG, PNG, WebP) |
- **Behavior**:
  - Checks if file is empty.
  - Generates UUID filename preserving original extension (`<uuid>.<ext>`).
  - Sends binary payload via Apache HttpClient 5 to Supabase Storage endpoint (`POST /storage/v1/object/Image/<uuid>.<ext>`).
  - Returns public URL: `https://<supabase-url>/storage/v1/object/public/Image/<uuid>.<ext>`.
- **Response Body**:
  ```json
  {
    "url": "https://abkxnzzffuojvumrhlrw.supabase.co/storage/v1/object/public/Image/93021f42-4dc9-4a9c-9c7f-94ad689b7b91.png"
  }
  ```
- **Status Codes**:
  - `200 OK`: File uploaded successfully.
  - `400 BAD_REQUEST`: Empty file (`{"error": "File is missings"}`).
  - `401 UNAUTHORIZED`: Authentication missing.
  - `413 PAYLOAD_TOO_LARGE`: Exceeds 5MB upload ceiling.
  - `500 INTERNAL_SERVER_ERROR`: Supabase API failure (`{"error": "Failed to upload image: ..."}`).

---

## 9. Real-Time Chat & Messaging Module

### 9.1 GET `/api/v1/tickets/{ticketId}/messages` (REST History)
- **Controller**: [ChatController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/ChatController.java#L31) (`getTicketMessages`)
- **Security / Roles**: Authenticated (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_TECHNICIAN`).
- **Path Parameters**: `ticketId` (Long).
- **Response Body**: `List<ChatMessage>` ordered by timestamp ascending:
  ```json
  [
    {
      "id": 1,
      "ticketId": 12,
      "senderId": 3,
      "sender": "Tech One",
      "content": "I am looking into your switch connection now.",
      "timestamp": "2026-10-22T11:00:00"
    }
  ]
  ```
- **Status Codes**: `200 OK`, `401 UNAUTHORIZED`.

### 9.2 STOMP `@MessageMapping("/chat")` (WebSocket Protocol)
- **Controller**: [ChatController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/ChatController.java#L23) (`sendMessage`)
- **Handshake Endpoint**: `GET /ws-chat` (configured with `.withSockJS()` in [WebSocketConfig.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/config/WebSocketConfig.java#L20)).
- **Client Destination (Send)**: `/app/chat`
- **Broker Subscription (Receive)**: `/topic/ticket/{ticketId}`
- **STOMP Message Body**:
  ```json
  {
    "ticketId": 12,
    "senderId": 3,
    "sender": "Tech One",
    "content": "Have you tried restarting the device?"
  }
  ```
- **Behavior**: Injects current timestamp `LocalDateTime.now()`, persists to PostgreSQL `chat_message` table, and broadcasts to topic `/topic/ticket/{ticketId}`.

---

## 10. Debug Endpoints (`/api/v1/debug`)

> [!CAUTION]
> All debug endpoints are configured as `permitAll()` in [SecurityConfig.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/security/SecurityConfig.java#L46). They require **NO** authentication headers and expose internal user data.

### 10.1 GET `/api/v1/debug/mytickets`
- **Controller**: [DebugController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/DebugController.java#L24)
- **Security**: Public (`permitAll`).
- **Description**: Hardcodes query for user `12L` with page 0, size 10 (`ticketService.getMyTickets(12L, 0, 10)`).
- **Status Codes**: `200 OK`.

### 10.2 GET `/api/v1/debug/test8`
- **Controller**: [DebugController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/DebugController.java#L29)
- **Security**: Public (`permitAll`).
- **Description**: Dumps tickets created by user ID 8 (`id`, `status`, `deleted`).
- **Status Codes**: `200 OK`.

### 10.3 GET `/api/v1/debug/tickets`
- **Controller**: [DebugController.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/DebugController.java#L43)
- **Security**: Public (`permitAll`).
- **Description**: Dumps all tickets in database (`id`, `title`, `status`, `createdBy`, `deleted`).
- **Status Codes**: `200 OK`.
