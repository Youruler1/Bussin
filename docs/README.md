# 📚 Bussin' Documentation Hub & Index

Welcome to the documentation repository for **Bussin' — India's Intercity Bus Ticket Booking Platform**. All specifications, architectural decision records, database schemas, API contracts, and verification matrices are organized below.

---

## 1. 🚀 Quickstart & Environment Setup (Root)

| Document / Script | Purpose |
|---|---|
| [**`../README.md`**](../README.md) | Main project overview, architecture summary, seed credentials, and R01–R20 summary matrix. |
| [**`../WINDOWS_SETUP.md`**](../WINDOWS_SETUP.md) | Complete step-by-step guide to installing prerequisites (`winget`), configuring `JAVA_HOME`, and building/running on a brand-new Windows 10/11 machine. |
| [**`../run-windows.ps1`**](../run-windows.ps1) | One-click Windows PowerShell launcher for both the Spring Boot backend (`:8080`) and React/Vite frontend (`:5173`). |
| [**`../scripts/verify-clean-setup.ps1`**](../scripts/verify-clean-setup.ps1) | Automated clean-environment verification script (runs backend JUnit 5 suite with `-DforkCount=0` + frontend Vite production build). |
| [**`../scripts/demo-walkthrough.ps1`**](../scripts/demo-walkthrough.ps1) | Live interactive CLI walkthrough of the full customer booking & cancellation lifecycle (`R16`). |

---

## 2. 🏛️ Architecture, Schema & API Contracts (`docs/`)

| Document | Purpose |
|---|---|
| [**`implementation-plan-and-adrs.md`**](./implementation-plan-and-adrs.md) | Executive feasibility audit, PDF conflict resolutions, Architectural Decision Records (**ADR-01** through **ADR-08**), and D1–D15 milestone breakdown. |
| [**`database-schema-and-ddl.md`**](./database-schema-and-ddl.md) | Mermaid Entity-Relationship diagram, relational invariants, half-open interval overlap math, and full PostgreSQL Flyway DDL (`V1__init_schema.sql`). |
| [**`openapi-spec.yaml`**](./openapi-spec.yaml) | Complete OpenAPI 3.0.3 REST contract covering Authentication, Catalog, Admin CRUD (Categories, Buses, Stations, Routes, Trips), Journey Search, Seat Maps, Atomic Bookings, and Admin Ledger. |

---

## 3. ✅ Traceability & Verification (`docs/`)

| Document | Purpose |
|---|---|
| [**`requirements-traceability-matrix.md`**](./requirements-traceability-matrix.md) | Bidirectional mapping from Saras Sir's PDF requirements (`REQ-ADM-*`, `REQ-CST-*`, `REQ-BKG-*`, `REQ-SEC-*`, `REQ-UI-*`) to backend/frontend code components and acceptance tests. |
| [**`test-verification-matrix.md`**](./test-verification-matrix.md) | Detailed test execution matrix mapping **R01 – R20** to the exact JUnit 5 integration/concurrency test classes in `src/test/java` and Windows Maven commands. |
| [**`acceptance-tests.md`**](./acceptance-tests.md) | Baseline R01–R20 pass criteria specification. |

---

## 4. 📋 Baseline Specifications & Governance (`docs/`)

| Document | Purpose |
|---|---|
| [**`reference/BusTicketBookingApp_Spec.pdf`**](./reference/BusTicketBookingApp_Spec.pdf) | Mandatory baseline project specification by Saras Sir (Java Training School). |
| [**`engineering-spec.md`**](./engineering-spec.md) | Approved engineering specification overlay (P0/P1/Deferred boundaries, interval locking, UTC/IST semantics, security, and API conventions). |
| [**`delivery-plan.md`**](./delivery-plan.md) | 15-day vertical-slice delivery schedule and quality gates. |
| [**`first-prompt.md`**](./first-prompt.md) | Initial project planning and audit prompt. |
| [**`../AGENTS.md`**](../AGENTS.md) | Workspace engineering rules and discipline guardrails. |
