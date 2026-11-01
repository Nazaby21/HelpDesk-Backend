# Backend Cleanup Report (Spring Boot)

## Overview
This document details all deletions, dead-code cleanups, removed deployment assets, and preserved seed data in the backend project (`helpdesk`).

---

## 1. Files & Directories Removed

| Path | Category | Reason for Deletion |
|---|---|---|
| `helpdesk/Dockerfile` | Deployment | Local-only execution; container deployment files are obsolete. |
| `helpdesk/src/test/` | Test Files | Phase 2 requirement: delete test folders/files that are test-only cruft. |
| `helpdesk/src/test/java/com/zaby/helpdesk/HelpdeskApplicationTests.java` | Test File | Deleted with `src/test/`. |
| `helpdesk/src/test/java/com/zaby/helpdesk/TicketCategoryRegressionTest.java` | Test File | Deleted with `src/test/`. |
| `helpdesk/src/test/java/com/zaby/helpdesk/controller/*.java` (5 integration test files) | Test Files | Integration test files used during initial dev; deleted with `src/test/`. |
| `helpdesk/src/test/resources/application-test.properties` | Test Config | Deleted with `src/test/`. |

---

## 2. Dependencies Cleaned in `pom.xml`

The following test-scoped dev dependencies were removed from [`pom.xml`](file:///D:/Course Wing/Final/fix/helpdesk/pom.xml):
- `org.springframework.boot:spring-boot-starter-data-jpa-test`
- `org.springframework.boot:spring-boot-starter-webmvc-test`
- `org.springframework.boot:spring-boot-starter-test`
- `org.springframework.security:spring-security-test`
- `com.h2database:h2`

---

## 3. Seed Data Preserved & Rationale

- **Admin Account**: `admin1@helpdesk.com` / `admin1234`
  - *Location*: [`DataSeeder.java`](file:///D:/Course Wing/Final/fix/helpdesk/src/main/java/com/zaby/helpdesk/config/DataSeeder.java)
  - *Rationale*: Critical seed account required to log in, create departments, manage categories, and triage tickets in a local development environment.
- **Default Departments**:
  - `IT` (Information Technology)
  - `HR` (Human Resources)
  - `Finance` (Finance & Accounting)
  - *Rationale*: Necessary foreign-key relations for user and technician assignments.
- **Database Safety**:
  - No database drop or recreate scripts were executed (`spring.jpa.hibernate.ddl-auto=update`).
  - PostgreSQL schema and records in `helpdesk_db` are 100% preserved.
