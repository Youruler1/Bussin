# Acceptance and Verification Matrix
| ID | Test | Pass criterion |
|---|---|---|
| R01 | Admin category CRUD | Create/read/update/deactivate; referenced category cannot be destructively removed |
| R02 | Bus CRUD | Capacity, registration uniqueness, category FK validated |
| R03 | Route/trip CRUD | Ordered stops, time monotonicity, bus seat snapshot validated |
| R04 | Customer registration/login | Password hashed; login/logout/session; unauthorized admin denied |
| R05 | Search filters | Origin, destination, date, departure window and prices correct |
| R06 | Partial-route seats | Same seat can be reserved on disjoint intervals |
| R07 | Overlap rejection | Same seat overlapping interval yields 409 |
| R08 | Concurrency | 20 concurrent attempts same segment produce exactly 1 success; DB has 1 active allocation |
| R09 | Connection search | No direct: returns time-feasible 1-transfer; rejects impossible connections |
| R10 | Multi-leg atomicity | Failure in second leg leaves zero new active allocations |
| R11 | Cancellation | Owner can cancel; repeated cancel has documented idempotent response; seat released |
| R12 | Price snapshot | Subsequent fare edits do not change booked price |
| R13 | Validation | Invalid input yields ProblemDetail and no stack trace |
| R14 | Security | Customer cannot read/cancel another's booking; admin APIs protected |
| R15 | Integration | Flyway applies to empty PostgreSQL; migrations re-run safely |
| R16 | E2E | Seed admin creates trip; customer searches, books, views and cancels |
| R17 | Frontend | Loading, empty, network failure and 409 conflict states |
| R18 | P1 holds | Expiry releases seat; simultaneous hold conflicts |
| R19 | P1 payment | Duplicate/delayed callbacks are idempotent; expiry recovery tested |
| R20 | Documentation | Clean-machine setup, seed, test commands and demo documented |

Evidence required per milestone: commands, exit codes, failing test names, screenshots/browser evidence when available, and file diffs. No green label for unrun tests.
