# 15-Day Delivery and Gates
D1 requirements traceability, ADRs, domain model and schema; approval gate.
D2 Maven/Spring/React/PostgreSQL/Flyway/CI scaffold, health, seed; smoke gate.
D3 auth/session/RBAC and category CRUD; security and CRUD tests.
D4 buses, seat layouts, stations and routes; integrity gate.
D5 trip scheduling, stop snapshots, fares, trip-seat mutex rows; migration gate.
D6 direct search including intermediate stops; query tests.
D7 connecting search and price aggregation; itinerary tests.
D8 booking single-leg and multi-leg transactional allocation; rollback tests.
D9 concurrency, interval overlap and idempotency; PostgreSQL stress tests.
D10 booking cancellation/history/ticket; lifecycle tests.
D11 customer frontend full path; Playwright E2E.
D12 admin frontend and validation; CRUD E2E.
D13 P1 seat holds/mock payment ONLY if P0 green; otherwise repair P0.
D14 CI, logging, docs, accessibility, performance measurement; regression gate.
D15 clean setup rehearsal, final test evidence, viva/demo, freeze.
Milestone gate: working backend build, frontend build, DB migration, tests, reviewed changes, no unreported blocker.
Cut order if behind: advanced analytics -> QR -> notifications -> mock payment -> holds; never cut mandatory search/booking/connecting routes, integrity, or tests.
