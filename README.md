# Helpdesk Support System - Backend API

This is the Spring Boot backend server for the Helpdesk Support System. It provides secure RESTful APIs, role-based access control, PostgreSQL database management, and high-performance image attachment handling via Supabase Storage.

## Tech Stack
* **Framework**: Spring Boot 3
* **Language**: Java 21
* **Database**: PostgreSQL
* **ORM**: Spring Data JPA & Hibernate
* **DTO Mapping**: MapStruct
* **Security**: Spring Security + JWT (JSON Web Tokens)
* **Storage**: Supabase Storage (S3-compatible via Apache HttpClient)
* **Build Tool**: Maven

## Features
* **Role-Based Authentication**: Secure authentication endpoints with token refresh support for `USER`, `TECHNICIAN`, and `ADMIN` roles.
* **Ticket Management System**: Full lifecycle support for creating, assigning, updating, and completing support tickets.
* **Multi-Image Attachments**: Support for attaching multiple images to a single ticket, automatically stored in an `@ElementCollection` mapping.
* **Incident Catalog**: APIs for managing Departments, Main Categories, and Sub-Categories for issue tracking.
* **Real-time Chat via WebSockets**: Bi-directional communication endpoints for communicating on a ticket.
* **Dashboard Stats**: Aggregated statistics API for pending, in-progress, and completed tickets.

## Getting Started

### Prerequisites
* Java 21+
* Maven 3+
* PostgreSQL 15+
* Supabase Account (for image bucket)

### Environment Variables
To run this application, you must define the following properties in your IDE environment variables, or update `src/main/resources/application.properties` directly:

```env
SUPABASE_URL=https://<your-project-id>.supabase.co
SUPABASE_KEY=<your-service-role-key>
SUPABASE_BUCKET=Image
```

### Installation

1. Copy the repository and build the project:
   ```bash
   mvn clean install
   ```
   *(Note: Ensure MapStruct annotation processors are enabled in your IDE so the DTO Mappers compile correctly).*

2. Create a PostgreSQL database called `helpdesk_db`.

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

Hibernate will automatically generate and update the schema (`ddl-auto=update`).

## Deployment
This backend is configured for simple cloud deployment (e.g., Railway, Render, Heroku). Be sure to configure the `PORT`, `DATABASE_URL`, `SUPABASE_KEY`, and `SUPABASE_URL` environment variables in your deployment dashboard.
