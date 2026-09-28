# GrievanceTrack – Citizen Complaint Tracking with SLA Escalation

A beginner-friendly Spring Boot backend where citizens file grievances, each grievance is
routed automatically to a department, and it is escalated to a senior officer if it is not
resolved within the category's SLA.

**Stack:** Java 25, Spring Boot 3.5.x, Maven, Spring Web, Spring Data JPA, Bean Validation, MySQL, Spring Scheduler, HTML/CSS/JavaScript.

## Entity relationships

```
Department 1 ──── N Category
Department 1 ──── N Grievance
Category   1 ──── N Grievance
Grievance  1 ──── N Escalation
```

| Entity | Fields |
|---|---|
| Department | id, name, description |
| Category | id, name, slaDaysLow, slaDaysMedium, slaDaysHigh, slaDaysCritical, department |
| Grievance | id, citizenName, description, location, severity, slaDurationDays, status, createdAt, slaDeadline, resolvedAt, rating, category, department |
| Escalation | id, grievance, escalatedToOfficer, reason, escalatedAt |
| GrievanceStatus (enum) | PENDING, IN_PROGRESS, RESOLVED, CLOSED |
| GrievanceSeverity (enum) | LOW, MEDIUM, HIGH, CRITICAL |

## Business rules (enforced in the service layer)

1. **SLA:** the category configures positive day durations for LOW, MEDIUM, HIGH, and CRITICAL. The backend selects by severity, stores the selected duration, and calculates `slaDeadline` from `createdAt`.
2. **Routing:** the grievance's department is always copied from `category.department`. Any `department` sent by the client is ignored.
3. **Status flow:** `PENDING -> IN_PROGRESS -> RESOLVED -> CLOSED` only. Skipping or going backwards returns `409` with a clear message. `resolvedAt` is set when the status becomes `RESOLVED`.
4. **Auto-escalation:** `SLAService` runs every 30 seconds. If `now > slaDeadline` and status is not RESOLVED/CLOSED, an `Escalation` is created with
   `escalatedToOfficer = "Senior <Department Name> Officer"`, `reason = "SLA exceeded"`, `escalatedAt = now`.
5. **No duplicate escalation:** `EscalationRepository.existsByGrievanceId` is checked before saving.
6. **Rating:** only when status is `CLOSED`, value 1–5, and only once.
7. **Severity:** citizens choose LOW, MEDIUM, HIGH, or CRITICAL when filing. Older API clients that omit it default to MEDIUM.
8. **Demo mode:** see below.

## Run it

Requirements: **JDK 25**, **Maven 3.9+**, and **MySQL 8+** on `localhost:3306` (or configure `DB_URL`).

1. Set the MySQL credentials in PowerShell (the password is not stored in the project):
   ```
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "your-mysql-password"
   $env:STAFF_API_KEY = "choose-a-private-staff-key"
   ```
   If needed, set the connection URL:
   ```
   $env:DB_URL = "jdbc:mysql://localhost:3306/grievance_track?createDatabaseIfNotExist=true"
   ```
2. Build and run:
   ```
   mvn clean package
   mvn spring-boot:run
   ```
3. Open `http://localhost:8080`.

## Web app

The page has Dashboard, File a Complaint, Track Complaint, All Complaints, Departments, and
Categories & SLA views. New databases get starter categories. Management forms use the REST
API and MySQL for create, edit, and delete operations. Legacy categories are migrated by
copying the former single SLA value to each severity.
Set `STAFF_API_KEY` before startup. The browser requests it only for status changes and
department/category writes; citizen filing, rating, tracking, and GET requests remain public.

## Demo mode

`app.demo-mode` in `application.properties` is read in **one place only** (`SLAService`).

| Value | Meaning |
|---|---|
| `false` | 1 SLA day = 1 real day |
| `true` (default) | 1 SLA day = **1 minute** internally |

So a category severity configured for 3 days escalates after about 3 minutes in demo mode
(plus up to 30 seconds for the next scheduler run). API responses and the UI display the
configured duration in **days**, never minutes.

## REST API

| Method | URL | Notes |
|---|---|---|
| POST | /departments | 201 |
| GET | /departments, /departments/{id} | 200 / 404 |
| PUT | /departments/{id} | 200 |
| DELETE | /departments/{id} | 204, 409 if in use |
| POST | /categories | body includes `name`, `department.id`, and positive `slaDaysLow`, `slaDaysMedium`, `slaDaysHigh`, `slaDaysCritical` values. Legacy `slaDays` is also accepted and applied to all four severities. |
| GET / PUT / DELETE | /categories, /categories/{id} | |
| POST | /grievances | body: `{"citizenName","description","location","severity":"HIGH","category":{"id":1}}` |
| GET | /grievances, /grievances/{id} | |
| PUT | /grievances/{id}/status | body: `{"status":"IN_PROGRESS"}` |
| PUT | /grievances/{id}/rating | body: `{"rating":5}` |
| GET | /escalations, /escalations/{id} | escalations are created automatically |

Errors are always JSON: `{ "timestamp": "...", "status": 409, "message": "..." }`.

## Testing with Postman

Import `GrievanceTrack.postman_collection.json` and run the requests in order (folder 1, then folder 2).
The collection stores ids in variables automatically. Step 6 is a manual wait of about 3 minutes (demo mode).
