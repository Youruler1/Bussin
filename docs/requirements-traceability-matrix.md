# Requirements Traceability Matrix (RTM)

**Project:** Bussin' — Full-Stack Bus Ticket Booking Platform  
**Baseline Specification:** [`docs/reference/BusTicketBookingApp_Spec.pdf`](./reference/BusTicketBookingApp_Spec.pdf)  
**Overlay:** [`docs/engineering-spec.md`](./engineering-spec.md) & [`docs/acceptance-tests.md`](./acceptance-tests.md)  

---

## 1. Bidirectional Traceability Table

| Req ID | Source Document | Mandatory Requirement Description | Engineering Spec Overlay Reference | Implemented Backend & Frontend Components | Acceptance Test ID | Milestone Day | Priority | Status |
|---|---|---|---|---|---|---|---|---|
| **REQ-ADM-01** | PDF § 3.1 | Admin Create, Read, Update Bus Categories (AC/Non-AC, Luxury, Sleeper, Seater) | Spec § 2, § 7 | `CategoryController`, `CategoryService`, `BusCategory`, `CategoryManager.jsx` | **R01** | Day 3 | P0 | ✅ Verified |
| **REQ-ADM-02** | PDF § 3.1 | Admin Delete Bus Categories (Reference-Safe) | Spec § 2, ADR-06 | `CategoryService.deleteCategory()` (soft-deactivates if buses exist; 409 Conflict) | **R01** | Day 3 | P0 | ✅ Verified |
| **REQ-ADM-03** | PDF § 3.1 | Admin Create, Read, Update Buses (brand, model, category, registration, capacity) | Spec § 2, § 5, § 7 | `BusController`, `BusService`, `Bus`, `Seat`, `BusManager.jsx` | **R02** | Day 4 | P0 | ✅ Verified |
| **REQ-ADM-04** | PDF § 3.1 | Admin Delete Buses (Reference-Safe) | Spec § 2, ADR-06 | `BusService.deleteBus()` (soft-deactivates if trips exist; 409 Conflict) | **R02** | Day 4 | P0 | ✅ Verified |
| **REQ-ADM-05** | Spec § 2, § 5 | Station and Route Management (ordered non-repeating stop sequences) | Spec § 5 | `StationController`, `RouteController`, `RouteService`, `RouteStop`, `StationRouteManager.jsx` | **R03** | Day 4 | P0 | ✅ Verified |
| **REQ-ADM-06** | PDF § 4, Spec § 5 | Bus Schedule / Trip creation & reference-safe deletion with stop/fare snapshots & mutex rows | Spec § 5, § 7, ADR-06 | `TripController`, `TripService`, `TripStop`, `TripStopFare`, `TripSeat`, `TripScheduler.jsx` | **R03** | Day 5 | P0 | ✅ Verified |
| **REQ-CST-01** | PDF § 3.2 | Customer Registration & Profile Management | Spec § 2, § 6, § 7 | `AuthController`, `UserController`, `AuthService`, `AppUser`, `AuthModal.jsx` | **R04** | Day 3 | P0 | ✅ Verified |
| **REQ-CST-02** | Spec § 6 | Customer & Admin Authentication (Login, Logout, HttpOnly Session Cookie, CSRF) | Spec § 6, ADR-05 | `SecurityConfig`, `CookieCsrfTokenRepository`, `frontend/src/api/client.js` | **R04** | Day 3 | P0 | ✅ Verified |
| **REQ-CST-03** | PDF § 3.2 | Route Search: Starting Station & Destination Station | Spec § 2, § 5, § 7 | `JourneyController`, `JourneySearchService.searchJourneys()`, `JourneySearch.jsx` | **R05** | Day 6 | P0 | ✅ Verified |
| **REQ-CST-04** | PDF § 3.2 | Route Search: Travel Date & Time-Window Filtering (`Asia/Kolkata`) | Spec § 5, ADR-04 | `JourneySearchService` (IST-to-UTC window conversion), `TripStopRepository` | **R05** | Day 6 | P0 | ✅ Verified |
| **REQ-CST-05** | PDF § 3.2 | Route Search: Intermediate Stop Pair Search ($boardOrder < alightOrder$) | Spec § 2, § 5 | `JourneySearchService` segment matching | **R05** | Day 6 | P0 | ✅ Verified |
| **REQ-CST-06** | PDF § 3.2 | Search Results Display: Available Buses, Seat Counts & Segment Prices (`BigDecimal`) | Spec § 5 | `JourneySearchResponse`, `TripStopFareRepository` | **R05** | Day 6 | P0 | ✅ Verified |
| **REQ-CST-07** | PDF § 3.2 | Connecting Routes Search: Triggered automatically if 0 direct trips found | Spec § 2, § 5 (1 transfer, 30m–8h layover), ADR-03 | `JourneySearchService.findConnectingJourneys()` | **R09** | Day 7 | P0 | ✅ Verified |
| **REQ-BKG-01** | PDF § 3.2, Spec § 5 | Visual Seat Map and Seat Availability per Segment $[boardIndex, alightIndex)$ | Spec § 5 | `TripController.getTripSeats()`, `TripService.getSeatMap()`, `SeatMapModal.jsx` | **R06**, **R16** | Day 6, Day 8 | P0 | ✅ Verified |
| **REQ-BKG-02** | PDF § 3.2, Spec § 5 | Book Seat Only When Availability Confirmed under Pessimistic Lock | Spec § 5 (`SELECT FOR UPDATE`), ADR-02 | `BookingService.createBooking()`, `TripSeatRepository.lockTripSeats()` | **R07**, **R08** | Day 8, Day 9 | P0 | ✅ Verified |
| **REQ-BKG-03** | Spec § 5 | Partial-Route Seat Resale on Disjoint Half-Open Intervals | Spec § 5, ADR-02 | `SeatAllocationRepository.findOverlappingAllocations()` | **R06** | Day 8 | P0 | ✅ Verified |
| **REQ-BKG-04** | Spec § 5 | Atomic Multi-Leg Connecting Route Booking with Deadlock-Free Lock Ordering | Spec § 5, ADR-03 (`ORDER BY trip_id, seat_id`) | `BookingService.createBooking()` (multi-leg rollback on Leg 2 conflict) | **R10** | Day 8 | P0 | ✅ Verified |
| **REQ-BKG-05** | Spec § 2, § 5 | Fare Snapshotting at Booking Creation | Spec § 5 | `BookingLeg.fareSnapshotInr`, `Booking.totalAmountInr` | **R12** | Day 8 | P0 | ✅ Verified |
| **REQ-BKG-06** | Spec § 7 | Idempotent Booking Creation (`Idempotency-Key` header) | Spec § 7 | `BookingController.createBooking()`, `BookingRepository.findByIdempotencyKey()` | **R08**, **R19** | Day 9 | P0 | ✅ Verified |
| **REQ-BKG-07** | Spec § 2, § 5 | Whole-Itinerary Booking Cancellation & Instant Seat Release | Spec § 5 | `BookingController.cancelBooking()`, `BookingService.cancelBooking()` | **R11** | Day 10 | P0 | ✅ Verified |
| **REQ-BKG-08** | Spec § 2, § 5 | Customer Booking History, Admin Ledger & Printable Ticket View | Spec § 5, § 7 | `BookingController`, `BookingHistory.jsx`, `PrintableTicket.jsx`, `BookingLedger.jsx` | **R14**, **R16**, **R17** | Day 10 | P0 | ✅ Verified |
| **REQ-SEC-01** | Spec § 6 | Ownership Authorization (Customer cannot read/cancel another customer's booking) | Spec § 6 | `BookingService` ownership check + `@PreAuthorize("hasRole('ADMIN')")` | **R14** | Day 3, Day 10 | P0 | ✅ Verified |
| **REQ-NFR-01** | PDF § 4, Spec § 9 | Normalized Relational Schema with Constraints, Flyway Migrations & Demo Seed | Spec § 9, `ddl-auto=validate` | `V1__init_schema.sql`, `V2__seed_reference_data.sql`, `DevDataInitializer.java` | **R15** | Day 2 | P0 | ✅ Verified |
| **REQ-NFR-02** | PDF § 6, Spec § 7 | RFC 9457 `ProblemDetail` Unified Exception Handling & MDC Request Tracing | PDF § 6, ADR-08 | `GlobalExceptionHandler`, `CorrelationIdFilter`, `BusNotFoundException`, `BusCategoryNotFoundException`, `CustomerNotFoundException` | **R13** | Day 3, Day 14 | P0 | ✅ Verified |
| **REQ-UI-01** | PDF § 1–2, Spec § 8 | Responsive Customer UI (Hero Search $\to$ Results $\to$ Seat Map $\to$ Book $\to$ Ticket) | Spec § 8 | `JourneySearch.jsx`, `SeatMapModal.jsx`, `BookingHistory.jsx`, `PrintableTicket.jsx` | **R16**, **R17** | Day 11 | P0 | ✅ Verified |
| **REQ-UI-02** | PDF § 3.1, Spec § 8 | Responsive Admin Management Interface (Categories, Fleet, Stations/Routes, Trips, Ledger) | Spec § 8 | `CategoryManager.jsx`, `BusManager.jsx`, `StationRouteManager.jsx`, `TripScheduler.jsx`, `BookingLedger.jsx` | **R16** | Day 12 | P0 | ✅ Verified |
| **REQ-P1-01** | Spec § 3, § 5 | Seat Hold Status (`HELD`) & Expiration Invariants in Overlap Queries | Spec § 3 (P1) | `SeatAllocation` (`HELD`, `holdExpiresAt`), `SeatAllocationRepository` | **R18** | Day 13 | P1 | ✅ Verified |
| **REQ-P1-02** | Spec § 3, § 5 | Payment Transaction Schema & Idempotency Key Deduplication | Spec § 3 (P1) | `payment_transaction` table, `Idempotency-Key` support on `/api/v1/bookings` | **R19** | Day 13 | P1 | ✅ Verified |
| **REQ-NFR-03** | Spec § 9, § 10 | Clean-Machine Windows Setup, Verification & Demo Walkthrough Scripts | Spec § 9, Acceptance Matrix R20 | `WINDOWS_SETUP.md`, `run-windows.ps1`, `scripts/verify-clean-setup.ps1`, `scripts/demo-walkthrough.ps1` | **R20** | Day 15 | P0 | ✅ Verified |

---

## 2. Mandatory Verification Compliance Summary
- **Zero Omission:** Every requirement in Saras Sir's PDF baseline (`docs/reference/BusTicketBookingApp_Spec.pdf`) and `docs/engineering-spec.md` is implemented and mapped above.
- **Dual-Mode Readiness:** Full reference data + 3 demo trips on `2026-11-01` are automatically seeded in both **Zero-Setup H2 mode** (`-Dspring-boot.run.profiles=local`) and **PostgreSQL mode** (`default` profile with Flyway `V1` + `V2` + `DevDataInitializer`).
