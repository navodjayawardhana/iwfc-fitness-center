# FitPulse – Intelligent Wellness and Fitness Center (IWFC)

Java prototype for CMP 7001 (Advanced Programming) PRAC 1. It manages fitness equipment, session scheduling and maintenance reporting for three roles: **Administrator**, **Instructor** and **Member**.

Built with **DDD + Clean Architecture + TDD**. The domain is plain Java. The console menu, the REST API and the React UI are interchangeable delivery adapters over the same `IwfcFacade`.

## Requirements
- JDK 25 (LTS) and Maven 3.9+ (backend)
- Node 22+ (frontend)

## Project layout
```
backend/    Java 25 + Maven: domain, application, infrastructure (console + Spring Boot REST API)
frontend/   Vite + React + TypeScript + Tailwind CSS UI
docs/       plan, todo, class diagram (PlantUML), report outline, video script
```

## Run
```bash
# backend: tests (237) + coverage report in backend/target/site/jacoco/index.html
cd backend
mvn test

# 1. console menu
mvn -q compile exec:java

# 2. REST API on http://localhost:8080
mvn spring-boot:run

# 3. React UI on http://localhost:5173 (needs the API running)
cd frontend
npm install
npm run dev          # npm run build = type-check (tsc) + production build
```

## Sign-in
Seeded demo users: `A-1` Administrator, `I-1` and `I-2` Instructors, `M-1` and `M-2` Members. **Demo password for every seeded account: `fitpulse-demo`** (local demo data only; new accounts choose their own password of 8+ characters).

- **Console / React:** sign in with user id + password. Administrators get user management (list, add, deactivate).
- **REST API:** `POST /api/login` with `{"userId":"A-1","password":"..."}` returns a token; send it on every other request as `Authorization: Bearer <token>`. `POST /api/logout` ends the session. Tokens expire after 8 hours and stop working if the account is deactivated.
- **How passwords are handled:** only a salted PBKDF2-HMAC-SHA256 hash (210,000 iterations) is stored, wrong id and wrong password give the same message, and the password is never printed or logged.
- **Limits (prototype):** sessions and users are in memory, so a restart signs everyone out and resets data; no rate limiting or account lockout; HTTPS is needed in real use.

## Storage: memory (default) or MySQL
The same code runs on either. `FITPULSE_STORAGE` chooses:

| Value | Effect |
|---|---|
| `memory` (default) | Sample data in memory, reset on every start. Used by tests and the quick demo |
| `mysql` | Everything (users, password hashes, equipment, sessions, bookings, maintenance, notifications, log) is stored in MySQL and survives restarts |

**One-time MySQL setup (for example WAMP):**
1. Start MySQL (WAMP icon > MySQL service running).
2. Edit `backend/db/init.sql`: put your own password where it says `CHANGE_ME` (do not commit that edit), then run it as a MySQL administrator (phpMyAdmin SQL tab, or `mysql -u root -p < backend/db/init.sql`).
3. Set the password for the app (never put it in the code or in git):
```powershell
$env:FITPULSE_STORAGE = "mysql"
$env:FITPULSE_DB_PASSWORD = "<the password you chose>"
# optional: $env:FITPULSE_DB_URL, $env:FITPULSE_DB_USER  (defaults: jdbc:mysql://localhost:3306/fitpulse, user fitpulse)
cd backend
mvn spring-boot:run          # REST API   (or: mvn -q compile exec:java  for the console)
```
Tables are created on the first start and the demo users, equipment and sessions are added once, only when the database has no users. Passwords are stored only as salted PBKDF2 hashes.

Automated tests cover the JDBC adapters without needing MySQL: the same contract test runs against the in-memory storage and against H2 in MySQL mode, and `DatabaseBootstrapTest` checks that data survives a restart.

## Architecture
```
Console menu ─┐
REST API ─────┼─> IwfcFacade ─> Use cases ─> Domain (pure Java)
React UI ─────┘        (application)            ^
                                   InMemoryRepository implements Repository<T, ID>
```
Dependencies point inward only. `ArchitectureTest` (ArchUnit) fails the build if the domain touches Spring, the application layer touches infrastructure, or a layer rule is broken.

| Layer | Package (under `backend/src/main/java`) | Contains |
|---|---|---|
| Domain | `com.iwfc.domain` | Entities, value objects, aggregate roots, custom exceptions, `Repository<T, ID>` port |
| Application | `com.iwfc.application` | Use cases, `IwfcFacade`, Observer-based notifications |
| Infrastructure | `com.iwfc.infrastructure` | In-memory and JDBC (MySQL) storage, composition root, console menu, Spring Boot REST adapter |

## Design patterns
| Category | Pattern | Where |
|---|---|---|
| Creational | Factory | `EquipmentFactory` (type-specific maintenance intervals) |
| Structural | Facade | `IwfcFacade` (one entry point for every delivery mechanism) |
| Behavioural | Observer | `NotificationService` (subject), `ReporterNotifier` and `AdminMaintenanceLog` (observers) |

## OOP and advanced Java
- **Abstraction / Polymorphism:** abstract `User` with `Administrator`, `Instructor`, `Member` answering the same permission methods differently.
- **Encapsulation:** private state, behaviour on the entities (`Equipment.logUsage`, `FitnessSession.book`, `MaintenanceRequest.assignTo`).
- **Generics + Collections:** `Repository<T, ID>` and `InMemoryRepository<T, ID>` (LinkedHashMap), `List`, `Set`, streams.
- **Value objects:** `TimeSlot`, `Location` (records).
- **Domain events:** `MaintenanceStatusChanged`.

## Custom exceptions
| Exception | Raised when | HTTP |
|---|---|---|
| `InvalidBookingException` | double-booking, outside operating hours (06:00–22:00), full or duplicate booking | 409 |
| `UnauthorizedAccessException` | a role does something it is not allowed to (e.g. Member reads the maintenance log) | 403 |
| `DuplicateEquipmentException` | equipment registered with an existing id | 409 |
| `InvalidStatusTransitionException` | maintenance workflow step skipped | 409 |
| `ResourceNotFoundException` | unknown equipment, session, request or user | 404 |
| `InvalidEquipmentOperationException` | bad usage hours, wrong equipment state, blank data | 400 |

## Testing (TDD)
Tests are written first, in ZOMBIES order (Zero, One, Many, Boundaries, Interface, Exceptions, Simple) and named `should_<result>_when_<condition>`. Git history shows the red → green pairs (`test: … (RED)` then `feat: … (GREEN)`).

**Intentional failing tests** (`IntentionalFailureDemoTest`, tagged `intentional-failure`) expect an illegal action to succeed, so they fail on purpose and the report shows the custom exception being thrown. They are excluded from the normal build. Run them alone:
```bash
cd backend
mvn test -Pshow-failure     # expected: 3 errors (InvalidBooking, UnauthorizedAccess, DuplicateEquipment)
```

`RobustnessVerificationTest` drives the system into each error condition on purpose and checks the right exception, its message, and that state is left unchanged.
