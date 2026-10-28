# Bus Booking Platform — Workspace Rules
Read `docs/engineering-spec.md`, `docs/acceptance-tests.md`, `docs/delivery-plan.md`, and the original PDF before planning.
The PDF is the immutable mandatory baseline; the engineering spec is the approved design overlay. In conflicts, preserve mandatory behavior and raise an explicit decision.
Use a modular monolith. Do not build microservices, Kafka, Kubernetes, or a real payment gateway.
Java 21, compatible Spring Boot 3.x, Maven, Spring Data JPA, Security, Flyway, PostgreSQL, React/Vite/JavaScript, JUnit 5, Mockito, Testcontainers, Playwright.
Use package-by-feature; constructor injection; thin controllers; service transactions; DTOs; RFC 9457 ProblemDetail; BigDecimal; OffsetDateTime/Instant; UTC storage; documented station local-time presentation.
Never expose JPA entities as JSON. Never use ddl-auto=update; use validated Flyway migrations.
Do not invent executed tests, screenshots, performance numbers, or completed features.
Security: no hardcoded credentials, secrets in env, ownership and role checks server-side, secure HttpOnly cookie session, CSRF protection, least privilege.
For every vertical slice: inspect -> plan -> implement -> migrate -> test -> document -> report actual commands/results. Avoid unrelated changes.
Ask for approval on destructive commands, irreversible migrations, credentials, external integrations, and major architecture changes.
At milestone boundaries produce: changed files, decisions, tests passed/failed/not run, reproduction commands, limitations, and next step.
Never delete or weaken a failing test to obtain a green build. Prefer working P0 over speculative P2 features.
