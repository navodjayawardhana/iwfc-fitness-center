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
1. `M-1` → option 5 (sessions) → 8 (book `S-1`) → 15 (notification).
2. `M-1` → 11 → "Access denied".
3. Switch user (16) → `I-1` → 10 (report `SB-04`, HIGH).
4. Switch → `A-1` → 11, 12 (assign `MR-001`), 13 (note), 14 (complete).
5. Switch → `I-1` → 15 (three notifications).
6. `A-1` → 2 (add `TM-01` again) → "Duplicate".
7. `I-2` → 6 (schedule in Studio A at 09:00) → "Invalid booking".

## Demo script (React + API)
Start `mvn spring-boot:run` (in backend) and `npm run dev` (in frontend), open http://localhost:5173, use the "Signed in as" switcher to walk through the same steps.
