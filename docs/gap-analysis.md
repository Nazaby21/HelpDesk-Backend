# Helpdesk Cross-Client Gap Analysis (Before & After Implementation)

> **Generated**: 2026-11-01
> **Source of Truth**: Spring Boot Backend (`D:\Course Wing\Final\fix\helpdesk`)
> **Full Detailed Report**: See `D:\Course Wing\Final\fix\helpdesk_mobile\docs\gap-analysis.md`

| Backend Feature / Endpoint | Method & Path | Used by Web? | Used by Mobile? | Role | Status After Implementation |
|---|---|---|---|---|---|
| **Auth: Login / Refresh / Logout** | `POST /api/v1/auth/{login,refresh,logout}` | Yes | Yes | Public | 🟢 Complete |
| **Users: Current Profile** | `GET /api/v1/users/me` | Yes | Yes | Authenticated | 🟢 Complete |
| **Users: Update Self Profile** | `PUT /api/v1/users/me` | Yes (`/pages/settings`) | Yes (`ProfileScreen`) | Authenticated | 🟢 **Implemented (P2)** |
| **Users: Admin CRUD** | `GET/POST/PUT/DELETE /api/v1/users/**` | Yes (`/users`) | Yes (`UserManagementScreen`) | `ADMIN` | 🟢 **Implemented (P2)** |
| **Tickets: Create Ticket** | `POST /api/v1/tickets` | Yes | Yes | `USER`, `ADMIN` | 🟢 Complete |
| **Tickets: Queues & History** | `GET /api/v1/tickets{,/my,/history}` | Yes (Status, Priority, Search) | Yes (Status, Priority, Search) | Role-scoped | 🟢 **Implemented (P1)** |
| **Tickets: Get by ID** | `GET /api/v1/tickets/{id}` | Yes | Yes | Authenticated | 🟢 Complete |
| **Tickets: Update Ticket** | `PUT /api/v1/tickets/{id}` | Yes (`/tickets/[id]/edit`) | Yes (`/tickets/:id/edit`) | Creator / `ADMIN` | 🟢 **Implemented (P1)** |
| **Tickets: Update Status** | `PATCH /api/v1/tickets/{id}/status` | Yes (`admin` & `technician`) | Yes (`ADMIN` & `TECHNICIAN`) | `ADMIN`, `TECHNICIAN` | 🟢 **Implemented (P1)** |
| **Tickets: Delete Ticket** | `DELETE /api/v1/tickets/{id}` | Yes (`TicketDetail` & Table) | Yes (`TicketDetailScreen`) | Creator / `ADMIN` | 🟢 **Implemented (P1)** |
| **Tickets: Dashboard & Monthly Stats** | `GET /api/v1/tickets/stats{,/monthly}` | Yes | Yes | Authenticated | 🟢 Complete |
| **Tickets: Activity Feed** | `GET /api/v1/tickets/activity` | Yes (Header Bell) | Yes (`/notifications`) | Authenticated | 🟢 **Implemented (P3)** |
| **Chat: Get & Post Messages** | `GET/POST /api/v1/tickets/{id}/messages` + STOMP | Yes | Yes | Authenticated | 🟢 **Implemented (P1)** |
| **Status Logs: Lifecycle Timeline** | `GET /api/v1/tickets/{id}/status-logs` | Yes (`TicketDetail.tsx`) | Yes (`TicketDetailScreen`) | Authenticated | 🟢 **Implemented (P1)** |
| **Audit History: Ticket Logs** | `GET /api/v1/history/ticket/{id}` | Yes (`TicketDetail.tsx`) | Yes (`TicketDetailScreen`) | Authenticated | 🟢 **Implemented (P3)** |
| **Categories: Full CRUD** | `GET/POST/PUT/DELETE /api/v1/categories/**` | Yes (`/categories`) | Yes (`CategoryManagementScreen`) | `ADMIN`, `TECHNICIAN` | 🟢 **Implemented (P2)** |
| **Departments: Full CRUD** | `GET/POST/PUT /api/v1/departments/**` | Yes (`/departments`) | Yes (`DepartmentManagementScreen`) | `ADMIN` | 🟢 **Implemented (P2)** |
| **Images: Upload File** | `POST /api/v1/images/upload` | Yes | Yes | Authenticated | 🟢 **Implemented (P1)** |
| **Notifications: Device Token** | `POST/DELETE /api/v1/notifications/device-token` | N/A | Yes (`ProfileScreen`) | Authenticated | 🟢 **Implemented (P3)** |
