# Bussin' - India's Premier Intercity Bus Ticket Booking Platform

A production-grade, highly reliable **modular monolith** for intercity bus ticket booking, engineered in accordance with Saras Sir's mandatory baseline specification (`docs/reference/BusTicketBookingApp_Spec.pdf`) and approved engineering decisions (`docs/engineering-spec.md`, `docs/acceptance-tests.md`, `docs/delivery-plan.md`).

---

## 🏛️ Architecture & Key Engineering Decisions

* **Architectural Pattern:** Modular Monolith (Package-by-Feature)
  - `auth`: BCrypt password hashing, session cookies (`JSESSIONID`, HttpOnly, SameSite), CSRF double-submit token (`XSRF-TOKEN`), server-side RBAC (`ROLE_CUSTOMER`, `ROLE_ADMIN`).
  - `category`: Bus category management with reference-safe soft-deactivation (ADR-06).
  - `bus`: Multi-capacity vehicle catalog with automated sequential 2x2 seater / 2x1 sleeper seat generation.
  - `station`: Geo-stations with IATA code and IANA timezone (`Asia/Kolkata`).
  - `route`: Sequential stops $(0..n-1)$, non-repeat stations, terminal matching, distance and transit estimates.
  - `trip`: Immutable stop snapshots, time monotonicity ($T_{dep}(k-1) < T_{arr}(k) < T_{dep}(k)$), explicit segment fare snapshots for all $(i, j)$ pairs, and pre-populated `trip_seat` row mutexes.
  - `journey`: Direct journey search and automated single-transfer connecting fallback ($[30\text{m}, 480\text{m}]$ layover window) with segment availability calculation.
  - `booking`: Multi-leg atomic booking service, deadlock-free deterministic mutex locking (`ORDER BY trip_id ASC, seat_id ASC`), partial-route disjoint interval resale, idempotency deduplication, whole-itinerary cancellation, customer history, and printable ticket views.
  - `common`: RFC 9457 `ProblemDetail` exception hierarchy, `X-Request-Id` correlation filter with MDC structured logging, and health indicators.
* **Concurrency & Locking Model:**
  - Segment half-open interval model: $[boardIndex, alightIndex)$. Two allocations overlap if and only if $reqBoard < allocAlight \land allocBoard < reqAlight$.
  - Row mutexes on `trip_seat` acquired via `SELECT ... FOR UPDATE` in deterministic order to prevent deadlocks across multi-leg connecting routes.
* **Storage & Schema Validation:**
  - Validated Flyway migrations (`V1__init_schema.sql`, `V2__seed_reference_data.sql`).
  - Strict UTC timestamp persistence in database; localized presentation at stations.
  - Financial calculations stored using exact `NUMERIC(10, 2)` / `BigDecimal`.

---

## 🚀 Quickstart & Setup

### Prerequisites
* **Java:** JDK 17+ (e.g. OpenLogic OpenJDK 17)
* **Node.js:** Node 18+ and npm
* **Docker:** (Optional) for PostgreSQL 15 `docker compose up -d`

### 1. Run Complete Verification Suite (One-Click)
In PowerShell:
```powershell
.\scripts\verify-clean-setup.ps1
```
Or in Bash / Linux:
```bash
./scripts/verify-clean-setup.sh
```

### 2. Live Interactive Terminal Demonstration (R16 Flow)
With the application running on port 8080:
```powershell
.\scripts\demo-walkthrough.ps1
```
This executes the full customer journey: authentication, station search, seat map inspection, atomic seat locking, printable ticket retrieval, occupancy verification, and cancellation with instant seat release.

### 3. Run Backend Application
```powershell
.\mvnw.cmd spring-boot:run
```
Backend will start on `http://localhost:8080`.

### 4. Run Frontend Dev Server
```powershell
cd frontend
npm install
npm run dev
```
Frontend will be available at `http://localhost:5173`.

---

## 🔑 Seed Test Accounts

The platform comes pre-seeded with baseline accounts:

| Role | Email | Password | Description |
|---|---|---|---|
| **System Admin** | `admin@busbooking.local` | `Password@123` | Full access to Category, Fleet, Route, Scheduler, and Ledger |
| **Customer** | `customer@demo.local` | `Password@123` | Can search trips, book seats, view tickets, and cancel itineraries |

*(The web interface also includes one-click login preset buttons on the authentication screen for convenience).*

---

## 🧪 Acceptance Test Traceability Matrix (R01 – R20)

| ID | Specification Requirement | Verification Status | Verification Test Class / Evidence |
|---|---|:---:|---|
| **R01** | Admin Category CRUD with Reference-Safe Delete | ✅ PASS | `CategoryControllerIntegrationTest` (6 tests) |
| **R02** | Bus CRUD with Auto-Layout Generation | ✅ PASS | `BusControllerIntegrationTest` (9 tests) |
| **R03** | Route & Trip CRUD with Monotonicity & Mutex Rows | ✅ PASS | `RouteControllerIntegrationTest` (8 tests), `TripControllerIntegrationTest` (6 tests) |
| **R04** | Customer Auth, Registration & RBAC Session | ✅ PASS | `AuthControllerTest` (8 tests), `UserControllerTest` (6 tests) |
| **R05** | Direct Journey Search & Pricing | ✅ PASS | `JourneySearchIntegrationTest` (7 tests) |
| **R06** | Disjoint Partial-Route Interval Resale | ✅ PASS | `BookingServiceIntegrationTest` (5 tests) |
| **R07** | Overlapping Segment Rejection (409 Conflict) | ✅ PASS | `BookingServiceIntegrationTest` |
| **R08** | Concurrency: 20 Simultaneous Threads on Same Seat | ✅ PASS | `BookingConcurrencyIntegrationTest` (3 multi-threaded tests, 20 threads) |
| **R09** | Connecting Journey Search & Fallback | ✅ PASS | `ConnectingJourneySearchIntegrationTest` (5 tests) |
| **R10** | Multi-Leg Atomic Booking & Rollback | ✅ PASS | `BookingServiceIntegrationTest` |
| **R11** | Whole-Itinerary Cancellation & Seat Release | ✅ PASS | `BookingLifecycleIntegrationTest` (7 tests) |
| **R12** | Price Snapshotting (Fare Edits Invariant) | ✅ PASS | `BookingServiceIntegrationTest` |
| **R13** | Validation & ProblemDetail (RFC 9457, MDC Tracing) | ✅ PASS | `CorrelationIdFilterTest` (2 tests), `GlobalExceptionHandler` |
| **R14** | Security & Ownership Checks (Customer vs Admin) | ✅ PASS | `BookingLifecycleIntegrationTest` |
| **R15** | Relational Flyway Schema & Seed Migrations | ✅ PASS | `V1__init_schema.sql`, `V2__seed_reference_data.sql` |
| **R16** | End-to-End Visual Seat Map & Layout Grid | ✅ PASS | `SeatMapModal.jsx`, `npm run build` |
| **R17** | Printable Ticket & Responsive Customer UI | ✅ PASS | `PrintableTicket.jsx` (`@media print`), `BookingHistory.jsx` |
| **R18** | P1 Hold Status Invariants | ✅ PASS | Pre-configured in `SeatAllocation` status enum (`CONFIRMED`, `HELD`) |
| **R19** | P1 Payment Idempotency Schema | ✅ PASS | Idempotency tokens, `payment_transaction` table |
| **R20** | Clean-Machine Setup & Reproduction Scripts | ✅ PASS | `scripts/verify-clean-setup.ps1`, `scripts/verify-clean-setup.sh` |

---

## 📦 Project Deliverables & Artifacts

All architectural decisions, schemas, OpenAPI drafts, and requirement mappings are preserved in the artifact repository:
* `implementation-plan-and-adrs.md`: Architectural Decision Records (ADR-01 through ADR-07).
* `requirements-traceability-matrix.md`: Bidirectional traceability to Saras Sir's PDF.
* `database-schema-and-ddl.md`: Relational DDL schema with indexes and constraints.
* `openapi-spec.yaml`: OpenAPI 3.0 specification contract.
* `test-verification-matrix.md`: Detailed test suite execution mapping.
