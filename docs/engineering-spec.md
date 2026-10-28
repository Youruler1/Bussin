# Engineering Specification v1.0 — Bussin' (India Intercity Bus Booking Platform)
## 1. Authority, Geography and Scope
- **Application Name:** **Bussin'** (India's Premier Intercity Bus Ticket Booking Monolith).
- **Geography & Operational Scope:** Tailored exclusively for intercity passenger transport in India. All stations, routes, schedules, passenger validations, and tax/fare displays adhere to Indian transit conventions.
- **Mandatory Baseline:** Original PDF in `docs/reference/BusTicketBookingApp_Spec.pdf` by Saras Sir is mandatory: admin category/bus CRUD; customer registration/profile; origin/destination/date/time-window search; prices; availability-gated booking; connecting routes if no direct trip; normalized PostgreSQL; Java/Spring Boot REST; React; exception handling; JUnit/Mockito.
- **Currency & Regional Formatting:** Indian Rupee (INR / ₹) with exact BigDecimal financial storage; Indian Standard Time (`Asia/Kolkata`, UTC+05:30); Indian 10-digit mobile contact numbers (+91).
- Engineering decisions below extend the PDF; do not misrepresent them as instructor requirements.

## 2. Committed P0
- Operator-independent admin role and customer role, login/logout, profile ownership; seed demo admin via environment-based bootstrap (no committed passwords).
- Bus categories and buses; stations; ordered routes and stops; scheduled trips with assigned buses and fares; CRUD with reference-safe deactivate/archive policies.
- Direct journey search by origin, destination, travel date, departure window, including intermediate stop pairs; available-seat count and price.
- Connecting journey search when direct results are absent, max 1 transfer for MVP, 30-minute minimum connection, max 8-hour wait, first departure in requested window; cross-midnight supported; return all valid one-transfer results within documented pagination/limit.
- Seat map, reservation, cancellation, booking history, printable ticket; all booking operations server-authoritative.
- Unit tests, PostgreSQL integration tests, concurrency tests, critical Playwright E2E.
## 3. Committed P1 if P0 green
- 10-minute seat holds and simulated payment (no external gateway), expiry sweeper, idempotent checkout; analytics occupancy/revenue; QR ticket, email outbox preview.
## 4. Deferred
Real payments/refunds, operator settlement, live GPS, dynamic pricing, coupons, multi-transfer booking, Redis/Kafka, production email/SMS, ML, mobile apps. Never build these before P0.

## 5. Domain decisions
- `Bus` physical vehicle; `Seat` physical seat belonging to bus; `Route` ordered station sequence; `Trip` dated service on route with assigned bus; `TripStop` immutable ordered departure/arrival snapshots for a scheduled trip; `Booking` itinerary and lifecycle; `BookingLeg` each trip segment; `SeatAllocation` selected physical seat and segment interval.
- `RouteStop` positions 0..n-1 unique per route, at least 2; `TripStop` stop index 0..n-1 with coherent strictly increasing event times; boarding index < alighting index.
- Seat interval is half-open [boardIndex, alightIndex); overlap iff a.start < b.end AND b.start < a.end.
- For a given trip+seat, overlapping HELD or CONFIRMED allocations cannot coexist. TripSeat row per trip/seat is the stable mutex: lock it via PESSIMISTIC_WRITE, then query and validate interval overlap in same transaction. This supports resale on nonoverlapping legs; do not store one global booked flag. Lock all selected TripSeat rows ordered by (tripId,seatId). Unique (tripId,seatId) on TripSeat; add supporting allocation indexes. Cancellation/expiry releases by status transition, retaining history.
- Booking for a connecting itinerary: one booking containing two legs, both allocated atomically in one database transaction; if either fails neither is confirmed. P0 uses immediate CONFIRMED booking without real payment; P1 adds HELD -> PAYMENT_PENDING -> CONFIRMED via simulated payment. Do not mix flows. Cancellation is whole-itinerary only in MVP.
- Fare is per trip+boarding/alighting pair; default can derive from full-trip fare proportional to stop-distance only if explicitly configured, rounded with BigDecimal; prefer explicit segment fare table for predictable tests. Snapshot total and per-leg fare in booking. Currency INR.
- Route stops may repeat a station only if explicitly supported later; MVP rejects repeats to avoid ambiguous journeys.
- Booking request carries tripId, boardingStopIndex, alightingStopIndex, seatId, passenger details and idempotency key; backend checks ownership, availability, fares and departure cutoff.
- One customer may book multiple passengers; passenger records are distinct from account profile.
- Admin changes to buses/routes must not rewrite historic trips/bookings; active trips use immutable stop snapshots. Prevent deleting referenced records; soft-deactivate where appropriate.
- Time instants persisted UTC (TIMESTAMPTZ), displayed Asia/Kolkata for seed data; API ISO 8601 offsets. Travel date and time window refer to departure at chosen boarding station in its local timezone (MVP stations in India).
- No payment card details stored.

## 6. Security decisions
Spring Security server-side session in Secure, HttpOnly, SameSite cookie in deployed HTTPS environment; CSRF enabled for mutations, CORS allowlist, BCrypt/Argon2 password hashing; local dev cookie settings documented separately. /api/v1/admin/** requires ADMIN; /api/v1/me/** requires authenticated account. Do not expose arbitrary customer IDs for private booking access.

## 7. API baseline
POST /api/v1/auth/register, /login, /logout; GET /api/v1/me, PUT /api/v1/me
GET /api/v1/categories; admin CRUD /api/v1/admin/categories
GET /api/v1/buses; admin CRUD /api/v1/admin/buses
GET /api/v1/stations; admin CRUD /api/v1/admin/stations
GET /api/v1/routes; admin CRUD /api/v1/admin/routes
GET /api/v1/trips/{id}; admin CRUD /api/v1/admin/trips
GET /api/v1/journeys/search?originStationId=&destinationStationId=&travelDate=&departureFrom=&departureTo=&page=&size=
GET /api/v1/trips/{id}/seats?boardIndex=&alightIndex=
POST /api/v1/bookings with Idempotency-Key header; GET /api/v1/me/bookings; GET /api/v1/bookings/{id}; POST /api/v1/bookings/{id}/cancel
P1: POST /api/v1/holds; POST /api/v1/payments/mock/confirm; GET /api/v1/admin/analytics.
Return 201 with Location for creations, 400 for invalid input, 401 unauthenticated, 403 forbidden, 404 missing or inaccessible resources, 409 seat/state conflict, 422 semantic booking errors where appropriate. Use ProblemDetail with stable error code, requestId and field errors.
Paginate lists and search. Default 20, max 100. Sorting allowlist only. No arbitrary client SQL.
All monetary JSON values decimal, not binary floats in backend; specify strings or JSON numbers consistently in generated OpenAPI.

## 8. UI
Responsive customer search -> results (direct/connecting) -> journey detail -> seat selection -> passenger entry -> confirm -> printable ticket; login/profile/history/cancel. Admin CRUD tables/forms with validation, schedules, occupancy and errors. Include loading, empty, 401/403, 409 stale seat, retry, keyboard and mobile states.

## 9. Nonfunctional
Docker Compose PostgreSQL; health/readiness; request IDs; structured logs with PII redaction; Flyway; OpenAPI; seed dataset with 6+ stations, 2+ routes, 3+ buses, 8+ trips, at least one valid connection, one no-service case. CI runs backend unit/integration and frontend build/lint/tests. No fabricated performance metrics.
## 10. Explicit limitations
No guaranteed real-time vehicle tracking; no real payment settlement; one transfer search only; cancellations are full-booking; only Indian station timezone for MVP. Document these transparently.
