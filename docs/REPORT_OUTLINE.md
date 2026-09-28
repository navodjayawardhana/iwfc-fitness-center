# Report outline (3000 words, A4, Arial or Times New Roman 12, 1" margins, 1/2" binding, single sided, Harvard)

Write this in your own words and check every claim against the code. Numbers to quote are in `docs/EVIDENCE.md`; diagrams are in `docs/diagrams/`. Word budget in brackets (total 3000; tables, figures and code excerpts are usually not counted, but confirm with the module leader).

## Front matter (not counted)
Cover sheet (name, batch, Cardiff Met and ICBT ids, declaration, signature), feedback sheet, contents, list of figures. File name `stXXXXXXXX_CMP7001_PRAC1`.

## 1. Introduction and problem domain analysis [250]
- The FitPulse (IWFC) scenario, the three actors, what the manual system gets wrong.
- Requirements traceability table: each brief requirement -> class -> test (equipment tracking, scheduling, maintenance, exceptions, patterns, testing).
- Scope and assumptions (operating hours 06:00-22:00, in-memory data by default, MySQL optional).
- Figure: `use-cases.puml`.

## 2. Object-oriented principles and domain modelling (LO1, LO4) [600]
- Abstraction and polymorphism: `User` with `Administrator`, `Instructor`, `Member` answering the same permission methods differently (no `instanceof`).
- Encapsulation and immutability: private state, behaviour on the entities, records for `TimeSlot`, `Location`, `Credential`.
- DDD: ubiquitous language, rich domain model (double-booking lives in `SessionSchedule`, the status cycle in `Equipment`, the workflow in `MaintenanceRequest`), entities vs value objects, aggregate roots, domain events.
- **Critical evaluation (LO1):** inheritance vs composition (why only `User` is inherited); permissions on `User` vs a separate policy class; rich vs anemic model; the cost of `restore(...)` factories.
- Figure: `class-diagram.puml`.

## 3. Advanced Java constructs and architecture (LO2, LO5) [500]
- Generics: `Repository<T, ID>`; collections (`LinkedHashMap`, `List`, `Set`, streams); records, `switch` expressions, pattern matching `instanceof`, `Optional`, `Clock` injection.
- Clean Architecture: dependency rule, ports and adapters, one facade for console, REST and React.
- Persistence as proof of the architecture: the same contract test runs against in-memory storage and JDBC (MySQL); the domain did not change when MySQL was added. Figures: `layers.puml`, `er-diagram.puml`.
- ArchUnit rules that fail the build if the rule is broken.

## 4. Design patterns implementation and justification (LO4) [550]
Same template for each: problem, solution, code (10 lines at most), benefit, limitation or alternative.
- Factory: `EquipmentFactory` (type-specific maintenance intervals), `UserFactory`.
- Facade: `IwfcFacade` (three delivery mechanisms, one entry point).
- Observer: `NotificationService` with `ReporterNotifier`, `AdminMaintenanceLog`, `AdminAlertNotifier` (the third was added without changing the subject).
- Figure: `sequence-fault-notify.puml`.

## 5. Secure coding and custom exception handling (LO3) [350]
- Table of custom exceptions: where thrown, who handles it, HTTP status, message.
- Unauthorized access (role checks in the domain), sign-in (PBKDF2 hashing, same message for every failure, expiring bearer tokens), input validation, state left unchanged on rejection.
- Honest limits (see EVIDENCE.md: in-memory sessions, no lockout, HTTPS needed).
- Figure: `sequence-sign-in.puml`.

## 6. Unit testing, TDD and robustness verification [500]
- TDD cycle, ZOMBIES order, naming, red/green commits in `git log --graph`.
- Test layers and counts; coverage (96.3% overall, gate at 90%); ArchUnit (9 rules).
- **Bugs found and fixed:** pick 4-5 from the table in `EVIDENCE.md`.
- Robustness verification: `IntentionalFailureDemoTest` and `mvn test -Pshow-failure` (screenshot of the 3 errors), `RobustnessVerificationTest`.

## 7. Critical evaluation, storytelling and visualisations [250]
- The story: brief -> design decisions -> what went wrong -> what changed.
- Screenshots of console, React UI (each role) and API responses; coverage chart.
- Limitations and future work (real authentication service, rate limiting, persistent reminder history, email notifications).
- Conclusion tied to the learning outcomes.

## Back matter (not counted)
References in Harvard style, only what you actually read: Martin (2017) *Clean Architecture*; Evans (2003) *Domain-Driven Design*; Beck (2002) *Test-Driven Development: By Example*; Gamma et al. (1994) *Design Patterns*; Bloch (2018) *Effective Java*; OWASP Password Storage Cheat Sheet; Oracle and Spring documentation; JUnit and ArchUnit user guides. Appendices: requirements matrix, endpoint table, full test list, git log, JaCoCo report.

## Scope note: class count (the brief says 10-15)
Concrete classes, counted from `backend/src/main`:
- **Core business system (what the brief means):** domain model 10 (`User`, `Administrator`, `Instructor`, `Member`, `Equipment`, `EquipmentFactory`, `FitnessSession`, `SessionSchedule`, `MaintenanceRequest`, `UserFactory`).
- **Application layer:** 12 (`IwfcFacade`, six use cases, `NotificationService`, three observers, `WellnessTips`).
- **Adapters (not part of the business rules):** about 27 (console 2, REST 9, JDBC 8, in-memory storage 3, hashing, configuration, entry point).
- Plus 8 custom exception classes, and records, enums and interfaces.

Argue it plainly in the report: the brief's prototype is the domain plus application core; the console, REST API, React UI, MySQL adapters and login are optional delivery and storage adapters that the architecture keeps outside it. If your module leader wants the total inside 10-15, say so early, because trimming now would remove features.
