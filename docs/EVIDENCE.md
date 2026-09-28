# Evidence pack (for report sections 6 and 7 and the video)

Every number below was produced by running the commands shown, on the `main` branch. Re-run them before you quote a number.

## How to reproduce
```bash
cd backend
mvn verify                    # 460 tests + coverage gate (>= 90% overall, >= 90% for domain and application packages)
mvn test -Pshow-failure       # the 3 intentionally failing tests: expected result is 3 errors
# coverage report: backend/target/site/jacoco/index.html
cd ../frontend
npm run check                 # tsc + ESLint + Vitest + production build
```

## Test suite
| What | Result |
|---|---|
| Backend tests (JUnit 5) | **460 passed, 0 failed** |
| Domain tests | 131 test methods (entities, value objects, roles, session rules, workflow) |
| Application tests | 135 test methods (use cases, facade end to end, notifications, authentication, reminders) |
| Infrastructure tests | 154 test methods (console, REST, persistence contract, bootstrap, hashing) |
| Architecture tests (ArchUnit) | 9 rules, all pass |
| Intentional failing tests | 3 (one per mandatory exception), run with `-Pshow-failure` |
| Frontend tests (Vitest) | 17 passed (dashboard and timetable logic) |
| Frontend static checks | `tsc` clean, ESLint clean |

## Coverage (JaCoCo)
| Scope | Instruction coverage |
|---|---|
| Whole backend | 96.3% (branch 87.0%) |
| `domain.model` | 96.2% |
| `application.*` (facade, notification, security, usecase) | 100% |
| Gate in `pom.xml` | build fails below 90% overall or below 90% in the domain or application packages |

## Architecture rules enforced by tests (`ArchitectureTest`)
1. Layers point inward (infrastructure -> application -> domain).
2. Domain depends on no outer layer.
3. Domain is free of frameworks (Spring, Jakarta, Jackson, `java.sql`, `java.io`).
4. Application does not depend on infrastructure.
5. Application is free of frameworks and database APIs.
6. Database code (`java.sql`, `javax.sql`, Spring JDBC) lives only in the JDBC adapter package.
7. All exceptions live in `domain.exception`.
8. Use case classes are named `...UseCase` and live in `application.usecase`.
9. `Repository` implementations live in infrastructure.

## TDD evidence
The git history shows red then green pairs: `test: ... (RED)` followed by `feat: ... (GREEN)` on each feature branch, merged into `main` with `--no-ff`. Useful command: `git log --oneline --graph --all`. Tests follow ZOMBIES order (Zero, One, Many, Boundaries, Interface, Exceptions, Simple) and are named `should_<result>_when_<condition>`.

## Bugs found and how they were fixed (real ones from this project)
| # | Where it showed up | What was wrong | Fix |
|---|---|---|---|
| 1 | Writing session tests | `FitnessSession` took an `Instructor`, so a test could not even pass a Member to prove access is refused | Took a `User` and checked the role in the domain (`ensureCanScheduleSessions`) |
| 2 | Console tests | The catch-all for unexpected errors also swallowed the "no more input" signal and printed `[Rejected] null` | Re-throw the end-of-input signal before the catch-all |
| 3 | Brief audit | `Equipment` allowed any status change (for example Operational straight to Under Maintenance) | Strict cycle Operational -> Faulty -> Under Maintenance -> Operational, with one allowed shortcut for preventative maintenance when due |
| 4 | Test for the fix above | Two requests on the same equipment broke the second assign and complete | The use case only starts or completes maintenance when the equipment is in the matching state |
| 5 | Use case tests | Cancel notice said "Cancelled:" but the test (and users) expect the word "cancelled" | Clearer message: "Your session was cancelled: ..." |
| 6 | ArchUnit | A private helper record in `application.usecase` broke the naming rule | The rule now applies to top-level classes, which is what it was meant for |
| 7 | ArchUnit | A default in-memory store nested inside the domain implemented `Repository`, breaking "implementations live in infrastructure" | Removed it from the domain; the composition root supplies the store |
| 8 | Security review | A wrong password and an unknown user could be told apart (message and timing) | Same message for both, and a dummy hash check for unknown users |
| 9 | Persistence design | A database returns copies, so a change is lost unless saved; the schedule relied on shared objects | `SessionSchedule` saves after every change; a test double that returns copies proves it |
| 10 | Running the console on Windows PowerShell | `-Dexec.mainClass=...` was split at the dot | `mainClass` set in `pom.xml`, so no flag is needed |
| 11 | Frontend tooling | `typescript-eslint` does not support TypeScript 7 yet | Tooling pinned to TypeScript 6.0 |
| 12 | ESLint | `setState` called inside an effect body | State is now set in the async callbacks only |
| 13 | Real MySQL check | Root login was refused (`using password: NO`), so the local root account has a password | Not a code bug; documented the dedicated `fitpulse` user setup and env-var password |

## Deliberate limits (for the "limitations" part of the report)
- Sessions (login tokens) are in memory: restarting the app signs everyone out.
- No rate limiting or account lockout; HTTPS is needed in real use.
- Reminder history is in memory: after a restart a member may be reminded again about a session still inside the window.
- Storage on MySQL was tested against H2 in MySQL mode; a run against a real MySQL server is still to be recorded.
- The React UI was type-checked, linted and built, but only checked by hand in a browser.
