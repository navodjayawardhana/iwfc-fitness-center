# 10-minute video walkthrough – talking points

Slide 1 of the PowerPoint must carry the video link (YouTube or OneDrive). Speak in your own words; these are prompts.

| Time | Show | Say |
|---|---|---|
| 0:00–0:45 | Title slide, repo on GitHub | Who you are, the IWFC problem, what the prototype does |
| 0:45–2:15 | Layer diagram + `docs/class-diagram.puml` | DDD + Clean Architecture, dependency rule, three delivery mechanisms over one facade |
| 2:15–3:45 | `User` and its subclasses, `Equipment`, `FitnessSession` in the IDE | Abstraction, encapsulation, polymorphism; rich domain model; value objects |
| 3:45–5:00 | `EquipmentFactory`, `IwfcFacade`, `NotificationService` | The three patterns: why each, what it replaced |
| 5:00–5:45 | `Repository<T, ID>` + `InMemoryRepository` | Generics and collections |
| 5:45–7:30 | **Live demo** (console or React + API) | Member books; Instructor reports a fault; Admin assigns, updates, completes; show notifications |
| 7:30–8:30 | Same demo, error cases | Member opens maintenance log (403 / Access denied); duplicate equipment; double-booked studio; outside operating hours |
| 8:30–9:30 | `mvn test`, JaCoCo report, git log | ZOMBIES order, red/green commits, coverage numbers, ArchUnit, RobustnessVerificationTest; **bugs found and how they were fixed** |
| 9:30–10:00 | Wrap-up | Limitations and what you would do next |

## Demo script (console)
Menu numbers: EQUIPMENT 1–5, SESSIONS 6–12, MAINTENANCE 13–18, ACCOUNT 19–20, 0 = exit. Options a role cannot use are dimmed and marked (for example "Administrator only").

1. `M-1` → 6 (sessions table) → 9 (book `S-1`) → 19 (notification).
2. `M-1` → 14 → "Access denied" (UnauthorizedAccessException).
3. 20 (switch user) → `I-1` → 13 (report `SB-04`, HIGH) → 14 (own requests) → 20.
4. Switch → `A-1` → 15 (assign `MR-001`), 16 (note), 17 (complete), 18 (activity log).
5. Switch → `I-1` → 19 (three notifications).
6. `A-1` → 2 (add `TREADMILL`, `TM-01` again) → "Duplicate".
7. `I-2` → 7 (schedule in Studio A at 09:00 next Monday) → "Invalid booking".
8. `I-1` → 5 (log 100 hours on `TM-01`) → switch `A-1` → 1 (maintenance due) → 19 (alert in inbox).

## Demo script (React + API)
Start `mvn spring-boot:run` (in backend) and `npm run dev` (in frontend), open http://localhost:5173, use the "Signed in as" switcher to walk through the same steps.
