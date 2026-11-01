# Backend Bug Report (Spring Boot)

## Overview
This document tracks all audited bugs, defects, and discrepancies identified in the backend (`helpdesk`), along with their resolution details and verification status.

---

### Bug BE-01: Null Pointer Exception on Token Refresh When User Has No Department
- **File**: [`AuthController.java`](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/controller/AuthController.java#L59-L62)
- **Problem**: When a user without an assigned department refreshed their JWT access token via `/api/v1/auth/refresh`, calling `tokenEntity.getUser().getDepartment().getId()` threw a `NullPointerException` (500 Internal Server Error).
- **Fix**: Added safe null check: `(tokenEntity.getUser().getDepartment() != null) ? tokenEntity.getUser().getDepartment().getId() : null`.
- **Status**: 🟢 FIXED & VERIFIED.

---

### Bug BE-02: Missing User ID and Image URL in AuthResponse
- **File**: [`AuthResponse.java`](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/dto/response/AuthResponse.java), [`AuthServiceImpl.java`](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/service/impl/AuthServiceImpl.java)
- **Problem**: `AuthResponse` previously omitted the user's primary key `id` and profile `imageUrl`, requiring clients to make secondary calls to `GET /api/v1/users/me` immediately upon login.
- **Fix**: Added `Long id` and `String imageUrl` fields to `AuthResponse` record, and populated them in `AuthServiceImpl.login()` and `AuthController.refresh()`.
- **Status**: 🟢 FIXED & VERIFIED.

---

### Bug BE-03: Restrictive CORS Origins Blocking Local Mobile/Web Clients
- **File**: [`CorsConfig.java`](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/config/CorsConfig.java)
- **Problem**: CORS was hardcoded to `http://localhost:3000`. Alternate loopback origins (`http://127.0.0.1:3000`), custom ports, and mobile dev tools were rejected with CORS errors.
- **Fix**: Configured `.allowedOriginPatterns("*")` with `.allowCredentials(true)` and all HTTP methods/headers permitted.
- **Status**: 🟢 FIXED & VERIFIED.

---

### Bug BE-04: Host Binding Restricted to Loopback
- **File**: [`application.properties`](file:///D:/Course Wing/Final/fix/helpdesk/src/main/resources/application.properties)
- **Problem**: Spring Boot defaulted to loopback address, preventing mobile devices connected to the local Wi-Fi / LAN from reaching the backend server.
- **Fix**: Explicitly set `server.address=0.0.0.0` and `server.port=8080`.
- **Status**: 🟢 FIXED & VERIFIED.
