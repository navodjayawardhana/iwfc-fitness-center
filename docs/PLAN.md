# FitPulse (IWFC) – CMP 7001 PRAC 1 Plan (target: 80+)

## Context
Assignment: Java prototype for the Intelligent Wellness and Fitness Center (IWFC) – equipment tracking, session scheduling, maintenance reporting. 75% weighting, 3000-word report (PDF), 10-min video, GitHub repo. Workspace `E:\Advance programingg` is empty except the brief (`CMP 7001_S1_PRAC1_25-26.docx`), so this is greenfield.
Approach: DDD + Clean Architecture (Uncle Bob) + TDD (ZOMBIES order) per the OneSyntax playbook. Domain is pure Java; console, REST API (Spring Boot) and React UI are swappable adapters. User must understand and explain every part (video/declaration) – each step is explained as it is built.

## Stack
Java 25 LTS (latest LTS; PC currently has Java 21 and no Maven, so install JDK 25 + Maven first), Maven, JUnit 5, JaCoCo (recent version with Java 25 support), ArchUnit (recent version), Spring Boot 4.x (adapter only), React (Vite) last, Git (GitHub) with red/green/refactor commits. Use modern Java where it fits the domain: records for Value Objects (`TimeSlot`), sealed types / enums, pattern-matching switch.
Node v22 and git already installed.

## Core classes (assignment's 10–15)
- domain: `User`(abstract)+`Administrator`/`Instructor`/`Member`, `Equipment`, `TimeSlot`(VO), `FitnessSession`, `MaintenanceRequest`, `EquipmentFactory`, 3 custom exceptions, repository interfaces
- application: `BookSessionUseCase`, `ReportFaultUseCase`, `AssignMaintenanceUseCase`, `IWFCFacade`, `NotificationService` + observer
- infrastructure: `InMemoryRepository<T,ID>` (generics+collections), `ConsoleMenu`
- Spring adapters (not counted): controllers, `ApiExceptionHandler`, `BeanConfig`, `IwfcApplication`

Patterns: Factory (creational), Facade (structural), Observer (behavioural); optional Strategy for maintenance-alert threshold.

## Phases
1. **Setup** – install JDK 25 + Maven, verify versions; Maven project `iwfc`, package layout `domain/application/infrastructure`, git init, JaCoCo + ArchUnit.
2. **Domain via TDD** – TimeSlot overlap → Equipment (unique ID, status, usage hours → maintenance alert) → FitnessSession (double-booking, operating hours, optional recurring weekly) → MaintenanceRequest (Pending→Assigned→Completed transitions) → User roles/polymorphism. Custom exceptions: InvalidBooking, UnauthorizedAccess, DuplicateEquipment. Include intentional failing test for robustness.
3. **Application layer** – use cases, Facade, Observer notifications on status change, access policy.
4. **Console menu** (minimum requirement complete) with pre-populated data.
5. **Spring Boot REST API** – thin controllers, exception → HTTP mapping (400/403/409).
6. **React UI** – equipment, booking, maintenance pages, role switcher. Lowest priority; marks are on architecture, not visuals.
7. **Report + video** – 3000 words, Harvard refs (Martin 2017, Evans 2003, Beck 2002), class/layer diagrams (PlantUML), ZOMBIES test table, bugs found/fixed, limitations; 10-min video script + PowerPoint with link on slide 1.

## Critical rules
- No `org.springframework` import in `domain` (enforced by ArchUnit test).
- Business rules live in domain models, use cases only orchestrate.
- Test names `should_<result>_when_<condition>`, Arrange-Act-Assert, domain coverage >90%.

## Verification
- `mvn test` all green (except the deliberate failing test, documented/isolated), JaCoCo report >90% domain.
- ArchUnit test passes.
- Run console demo covering every functional requirement bullet + each exception.
- Hit REST endpoints (curl/Postman), then React UI end-to-end.
- Checklist against brief: 10–15 classes, 3 pattern categories, collections+generics, 3 exceptions, JUnit scheduling + workflow tests, GitHub history, report format rules, video content.

## Open items
- Deadline (on Moodle, not in brief), student ID for file naming (`stXXXXXXXX_CMP7001_PRAC1`), GitHub account/repo name.

Progress is tracked in `TODO.md`.
