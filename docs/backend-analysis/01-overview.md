# Phase 1: Backend System Overview & Architecture Discovery

This document provides a comprehensive technical overview of the Spring Boot backend supporting the Helpdesk Support System. It analyzes the build configuration, technology stack, dependency graph, application properties, and deployment runtime so that a Flutter mobile engineering team can effectively interface with the system.

---

## 1. System Summary & Tech Stack Discovery

The backend is an enterprise support ticket management API built with Java and Spring Boot. It handles ticket life-cycles, category trees, department structures, real-time ticket messaging, and multi-image attachment uploads backed by Supabase Storage.

| Component | Detected Version / Specification | Source Citation |
| :--- | :--- | :--- |
| **Framework** | Spring Boot **4.0.3** | [pom.xml](file:///D:/Course Wing/Final/fix/helpdesk/pom.xml#L8) & [run.log](file:///D:/Course Wing/Final/fix/helpdesk/run.log#L38) |
| **Language** | Java **21** (Temurin 21.0.1) | [pom.xml](file:///D:/Course Wing/Final/fix/helpdesk/pom.xml#L30) & [Dockerfile](file:///D:/Course Wing/Final/fix/helpdesk/Dockerfile#L2) |
| **Servlet Container** | Apache Tomcat **11.0.18** | [run.log](file:///D:/Course Wing/Final/fix/helpdesk/run.log#L47) |
| **Build Tool** | Apache Maven 3.9.6 (Maven Wrapper provided) | [pom.xml](file:///D:/Course Wing/Final/fix/helpdesk/pom.xml) & [mvnw.cmd](file:///D:/Course Wing/Final/fix/helpdesk/mvnw.cmd) |
| **Database** | PostgreSQL 15+ (Production/Dev), H2 in-memory (Test) | [application.properties](file:///D:/Course Wing/Final/fix/helpdesk/src/main/resources/application.properties#L19) |
| **ORM / Persistence**| Spring Data JPA / Hibernate Core **7.2.4.Final** | [pom.xml](file:///D:/Course Wing/Final/fix/helpdesk/pom.xml#L35) & [run.log](file:///D:/Course Wing/Final/fix/helpdesk/run.log#L50) |
| **Security Framework** | Spring Security 6+ with Stateless JWT (`jjwt-api:0.11.5`) | [SecurityConfig.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/security/SecurityConfig.java) |
| **Validation** | Jakarta Bean Validation (`spring-boot-starter-validation`) | [pom.xml](file:///D:/Course Wing/Final/fix/helpdesk/pom.xml#L75) |
| **DTO Mapping** | MapStruct **1.5.5.Final** with Lombok MapStruct Binding 0.2.0 | [pom.xml](file:///D:/Course Wing/Final/fix/helpdesk/pom.xml#L71) |
| **File Storage** | Supabase Storage (REST / S3 via Apache HttpClient 5) | [SupabaseStorageService.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/service/SupabaseStorageService.java) |
| **Messaging / Realtime**| Spring WebSocket with STOMP message broker + SockJS | [WebSocketConfig.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/config/WebSocketConfig.java) |
| **API Documentation** | **None** (Springdoc / OpenAPI / Swagger are NOT configured) | [pom.xml](file:///D:/Course Wing/Final/fix/helpdesk/pom.xml) |
| **Schema Migrations** | **None** (Hibernate `ddl-auto=update` is used directly; no Flyway/Liquibase) | [application.properties](file:///D:/Course Wing/Final/fix/helpdesk/src/main/resources/application.properties#L26) |

> [!NOTE]
> While the repository `README.md` references Spring Boot 3, the project configuration in [pom.xml](file:///D:/Course Wing/Final/fix/helpdesk/pom.xml#L8) specifies `spring-boot-starter-parent` version `4.0.3` running on Apache Tomcat 11 and Hibernate 7.2.4.

---

## 2. Dependency Inventory & Architecture Purpose

The following dependencies are declared in [pom.xml](file:///D:/Course Wing/Final/fix/helpdesk/pom.xml):

| Dependency Group & Artifact | Scope | Purpose in Architecture |
| :--- | :--- | :--- |
| `org.springframework.boot:spring-boot-starter-data-jpa` | Compile | Manages PostgreSQL persistence, Spring Data Repositories, JPA transaction management, and entity mapping. |
| `org.springframework.boot:spring-boot-starter-web` & `spring-boot-starter-webmvc` | Compile | Provides Spring MVC REST controllers, JSON Jackson serialization, and embedded Tomcat container. |
| `org.springframework.boot:spring-boot-starter-security` | Compile | Provides authentication filter chain, password hashing (`BCrypt`), and authorization interceptors. |
| `org.springframework.boot:spring-boot-starter-validation` | Compile | Provides Jakarta Bean Validation annotations (`@NotBlank`, `@NotNull`, etc.). |
| `org.springframework.boot:spring-boot-starter-websocket` | Compile | Powers real-time bi-directional STOMP chat protocol over WebSocket connections. |
| `org.postgresql:postgresql:42.7.9` | Runtime | PostgreSQL JDBC driver for establishing database connections. |
| `io.jsonwebtoken:jjwt-api:0.11.5`<br>`io.jsonwebtoken:jjwt-impl:0.11.5`<br>`io.jsonwebtoken:jjwt-jackson:0.11.5` | Compile / Runtime | Creates, signs, and parses JSON Web Tokens (HS256) for mobile/web authorization. |
| `org.mapstruct:mapstruct:1.5.5.Final` | Compile | Generates high-performance compile-time type-safe mappers between JPA Entities and DTO records. |
| `org.projectlombok:lombok` | Provided | Generates getters, setters, constructors, builders, and SLF4J loggers at compile time. |
| `org.apache.httpcomponents.client5:httpclient5` | Compile | Executes HTTP POST requests to upload binary image files directly into Supabase Storage buckets. |
| `org.springframework.boot:spring-boot-devtools` | Runtime | Provides automatic application restart and live reload during local development. |
| `com.h2database:h2` | Test | In-memory relational database configured for unit and integration testing. |
| `org.springframework.boot:spring-boot-starter-test` | Test | JUnit 5, AssertJ, Hamcrest, and Spring Test runner for automated test suites. |
| `org.springframework.security:spring-security-test` | Test | Mock security contexts (`@WithMockUser`) for MockMvc controller tests. |

---

## 3. Configuration Profiles & Settings Summary

The application maintains configuration across two files: `application.properties` (runtime/dev) and `application-test.properties` (automated tests).

### 3.1 Primary Profile (`src/main/resources/application.properties`)

```properties
spring.application.name=helpdesk
server.port=8080

# Supabase Storage Configuration
supabase.url=https://abkxnzzffuojvumrhlrw.supabase.co
supabase.key=${SUPABASE_KEY:}
supabase.bucket=Image

# Multipart File Limits
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB

# PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/helpdesk_db
spring.datasource.username=postgres
spring.datasource.password=****** [MASKED]

# Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
```

Key observations for mobile client integration:
1. **Server Port & Host**: Defaults to port `8080`. When testing on Android emulators, `http://10.0.2.2:8080` must be used; on iOS simulators, `http://localhost:8080`.
2. **File Size Limit**: Both `max-file-size` and `max-request-size` are set to `5MB`. If a Flutter client attempts to upload an uncompressed camera photograph exceeding 5MB, Tomcat rejects the connection with HTTP 413 Payload Too Large.
3. **Supabase Bucket**: Images are stored in bucket `Image` and accessed via public URLs (`https://<supabase-url>/storage/v1/object/public/Image/<file-uuid>`).
4. **Active Profile**: In logs, `dev` is shown active by default.

### 3.2 Test Profile (`src/test/resources/application-test.properties`)

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=****** [MASKED]
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
```

---

## 4. Architectural Component Diagram

The following diagram illustrates the runtime interaction between the Flutter client, Spring Boot backend, and external cloud infrastructure:

```mermaid
flowchart TD
    subgraph MobileClient["Flutter Mobile Client"]
        FlutterUI["Flutter UI (Widgets/Bloc/Riverpod)"]
        SecureStorage["flutter_secure_storage (Access/Refresh Tokens)"]
        DioClient["Dio / HTTP Client (Bearer Auth Interceptor)"]
        StompClient["STOMP WebSocket Client"]
    end

    subgraph SpringBootBackend["Spring Boot 4.0.3 Backend (Port 8080)"]
        SecurityFilter["JwtAuthenticationFilter (OncePerRequestFilter)"]
        Controllers["REST Controllers (/api/v1/*)"]
        WebSocketBroker["STOMP Message Broker (/ws-chat, /topic)"]
        Services["Service Layer (TicketService, UserService, etc.)"]
        Mappers["MapStruct Mappers"]
        Repositories["Spring Data JPA Repositories"]
    end

    subgraph ExternalInfra["External Infrastructure & Data Layer"]
        PostgresDB[("PostgreSQL Database (helpdesk_db)")]
        SupabaseStorage[("Supabase Storage (Bucket: Image)")]
    end

    FlutterUI --> DioClient
    FlutterUI --> StompClient
    DioClient <-->|REST HTTP Requests (Bearer Token)| SecurityFilter
    SecurityFilter --> Controllers
    StompClient <-->|WebSocket STOMP Handshake| WebSocketBroker
    Controllers --> Services
    Services --> Mappers
    Services --> Repositories
    Repositories <--> PostgresDB
    Services -->|Apache HttpClient 5| SupabaseStorage
```

---

## 5. Build & Packaging Pipeline

- **Docker Container**: Defined in [Dockerfile](file:///D:/Course Wing/Final/fix/helpdesk/Dockerfile). Multi-stage build using `maven:3.9.6-eclipse-temurin-21` for compilation and `eclipse-temurin:21-jre-alpine` for the minimal runtime container running as an unprivileged user (`spring:spring`).
- **Initial Data Seeder**: [DataSeeder.java](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/config/DataSeeder.java) executes on startup via `CommandLineRunner`. It seeds three default departments (`IT`, `HR`, `Finance`) and one initial admin account (`admin1@helpdesk.com` / `admin1234` with role `ADMIN`).
