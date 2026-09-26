# Report outline (3000 words, A4, Arial or Times New Roman 12, Harvard references)

Write this in your own words and check every claim against the code. Word budget in brackets.

## Cover sheet + feedback sheet (not counted)
Student ID, batch, declaration, file name `stXXXXXXXX_CMP7001_PRAC1`.

## 1. Introduction and problem analysis [250]
- The IWFC scenario, the three actors, what the manual system gets wrong.
- Functional requirements → where each is implemented (table: requirement, class, test).
- Scope and assumptions: in-memory data, demo-level identity, operating hours 06:00–22:00.

## 2. Design approach [450]
- Why DDD (ubiquitous language: Equipment, Session, MaintenanceRequest), rich domain model vs anemic model.
- Why Clean Architecture: dependency rule, ports and adapters, three delivery mechanisms share one facade.
- Aggregates and value objects: `Equipment`, `SessionSchedule` (roots); `TimeSlot`, `Location` (records).
- Layer diagram + class diagram (`docs/class-diagram.puml`).
- **Class count:** state honestly how the classes group (see "Scope note" below).

## 3. Object-oriented principles (LO1) [400]
- Abstraction and polymorphism: `User` → `Administrator`, `Instructor`, `Member` (permission methods; no `instanceof`).
- Encapsulation: private state, behaviour inside entities, `Equipment.logUsage` rules.
- Inheritance vs composition: why only `User` is inherited; sessions compose equipment and instructor.
- Critical evaluation: the trade-offs (e.g. permission methods on `User` vs a separate policy class).

## 4. Generics, collections and advanced Java (LO2) [300]
- `Repository<T, ID>` / `InMemoryRepository<T, ID>` (one class for every entity).
- `List`, `Map`/`LinkedHashMap`, streams, records, sealed/immutable value objects, `Optional`.

## 5. Design patterns [450]
- Factory: `EquipmentFactory` (problem, solution, benefit, alternatives such as Builder).
- Facade: `IwfcFacade` (console, REST and React never touch use cases directly).
- Observer: `NotificationService` + observers, domain events `MaintenanceStatusChanged`.
- For each: intent, UML fragment, code excerpt (≤10 lines), why it fits, limitation.

## 6. Exception handling and security (LO3) [300]
- Table of the custom exceptions, where thrown, how handled (console messages, HTTP status mapping).
- Unauthorized access design (role permissions in the domain, `ensureCan…`), limits of the `X-User-Id` header.

## 7. Testing and TDD [450]
- ZOMBIES order, naming convention, red → green → refactor, commit history evidence.
- Test pyramid: domain unit tests, use case tests with in-memory repos, facade end-to-end, ConsoleMenu scripted tests, REST MockMvc tests, ArchUnit rules.
- Coverage (JaCoCo): quote the real numbers from `target/site/jacoco/index.html`.
- **Bugs found and fixed** (use real ones from the history): e.g. a stray `git add` put the implementation into the RED commit and was redone; test used `Instructor` type so a Member could not be passed, fixed by taking `User`; cancel notification wording. Add your own.
- Robustness verification: `RobustnessVerificationTest`.

## 8. Critical evaluation, limitations and conclusion [250]
- What works well, what you would change (persistence, real authentication, concurrency, notifications by email).
- Lessons learned.

## References (Harvard, not counted)
Suggested: Martin (2017) *Clean Architecture*; Evans (2003) *Domain-Driven Design*; Beck (2002) *Test-Driven Development: By Example*; Gamma et al. (1994) *Design Patterns*; Bloch (2018) *Effective Java*; plus the Oracle Java docs and the ArchUnit/JUnit user guides. Only cite what you actually read.

## Scope note: class count
The brief asks for 10–15 classes. The concrete classes are:
- **Entities and aggregates (9):** `User`, `Administrator`, `Instructor`, `Member`, `Equipment`, `FitnessSession`, `SessionSchedule`, `MaintenanceRequest`, plus value objects `TimeSlot`/`Location` (records)
- **Pattern and service classes (7):** `EquipmentFactory`, `IwfcFacade`, `NotificationService`, `ReporterNotifier`, `AdminMaintenanceLog`, three use cases
- **Infrastructure (3):** `InMemoryRepository`, `IwfcBootstrap`, `ConsoleMenu`

That is more than 15 if every class is counted. Decide with your module leader or justify it in the report (core domain vs adapters), or merge classes if you want to stay inside the range.
