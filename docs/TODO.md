# IWFC – TODO

### Setup
- [x] Install JDK 25 LTS, set JAVA_HOME, verify `java -version`
- [x] Install Maven, verify `mvn -v`
- [x] Create Maven project `iwfc` (packages domain / application / infrastructure)
- [ ] `git init`, create GitHub repo, first commit
- [x] Add JUnit 5, JaCoCo, ArchUnit to pom.xml
### Domain (TDD, ZOMBIES: red → green → refactor commits)
- [x] TimeSlot (record VO) + overlaps()
- [x] Equipment: unique ID, status, location, usage hours, maintenance alert
- [x] EquipmentFactory (Creational pattern)
- [x] FitnessSession: double-booking, operating hours, (optional) recurring weekly
- [x] MaintenanceRequest: urgency, Pending→Assigned→Completed transitions
- [x] User (abstract) + Administrator / Instructor / Member (polymorphism)
- [x] Custom exceptions: InvalidBooking, UnauthorizedAccess, DuplicateEquipment
- [ ] Intentional failing test for robustness verification
### Application
- [x] Repository interfaces (domain) + generic InMemoryRepository<T,ID>
- [x] BookSessionUseCase, ReportFaultUseCase, AssignMaintenanceUseCase
- [x] Observer notifications on maintenance status change
- [x] IWFCFacade (Structural pattern)
- [x] Access policy (role checks, via User polymorphism)
- [x] Use case integration tests (in-memory repos)
### Delivery
- [x] ConsoleMenu + pre-populated data (minimum requirement done)
- [ ] ArchUnit test: domain has no Spring/infrastructure dependency
- [ ] JaCoCo report, domain coverage >90%
- [ ] Spring Boot REST API + ApiExceptionHandler
- [ ] React (Vite) UI: equipment, booking, maintenance, role switcher
### Report & Video
- [ ] Class diagram + layer diagram (PlantUML)
- [ ] Report 3000 words: rationale, patterns, ZOMBIES test table, bugs fixed, limitations, Harvard refs
- [ ] Format check: A4, Arial/TNR 12, margins, cover + feedback sheet, PDF, `stXXXXXXXX_CMP7001_PRAC1`
- [ ] PowerPoint (video link on slide 1) + 10-min video script
- [ ] Record video, upload YouTube/OneDrive
- [ ] Submit: Moodle/Turnitin PDF (before 2:00pm) + ICBT SIS Word version

