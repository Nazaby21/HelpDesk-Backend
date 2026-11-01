# Helpdesk Support System - Backend API (Local Setup)

This is the Spring Boot backend server for the Helpdesk Support System. It provides secure RESTful APIs, role-based access control, PostgreSQL persistence, and WebSocket chat.

## Tech Stack
* **Framework**: Spring Boot 4.0.3
* **Language**: Java 21
* **Database**: PostgreSQL (`localhost:5432/helpdesk_db`)
* **ORM**: Spring Data JPA & Hibernate
* **DTO Mapping**: MapStruct
* **Security**: Spring Security + JWT (HS256)
* **Build Tool**: Maven (`mvnw`)

## Running Locally

### 1. Prerequisites
* Java 21 JDK installed and configured (`JAVA_HOME`).
* PostgreSQL running locally on port `5432` with database `helpdesk_db`.

### 2. Configuration
Configuration is defined in `src/main/resources/application.properties` (or optionally `.env`):
* `server.port=8080`
* `server.address=0.0.0.0` (binds to all interfaces so physical phones on local Wi-Fi can connect)
* `spring.datasource.url=jdbc:postgresql://localhost:5432/helpdesk_db`
* `spring.datasource.username=postgres`
* `spring.datasource.password=123`

### 3. Build & Start the Server
```powershell
cd helpdesk
.\mvnw.cmd spring-boot:run
```
Or build the JAR:
```powershell
.\mvnw.cmd clean package
java -jar target/helpdesk-0.0.1-SNAPSHOT.jar
```

### 4. Default Seed Account
* **Email**: `admin1@helpdesk.com`
* **Password**: `admin1234`
* **Role**: `ADMIN`

### 5. Windows Firewall
To allow a physical mobile device on the same Wi-Fi to reach port 8080:
```powershell
netsh advfirewall firewall add rule name="Helpdesk Backend 8080" dir=in action=allow protocol=TCP localport=8080
```
