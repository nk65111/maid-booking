# Maid Booking Service

Backend for a home-services platform: onboard maids, search for available ones, book them
(instant / scheduled / recurring), pay, and cancel with a refund policy.

Java 17+, Spring Boot 3.5, in-memory storage.

## Run

```bash
mvn spring-boot:run          # starts on http://localhost:8080
mvn test                     # unit + API tests
```

Swagger UI: http://localhost:8080/swagger-ui.html

## API

| Operation | Endpoint |
|---|---|
| Onboard maid | `POST /maids` |
| Get maid | `GET /maids/{id}` |
| Search | `GET /maids/search?locality=&serviceType=&start=&durationMinutes=&minRating=&maxPrice=&skills=&gender=` |
| Book | `POST /bookings` |
| Get booking | `GET /bookings/{id}` |
| Pay | `POST /bookings/{id}/payments` (header `Idempotency-Key`) |
| Cancel whole booking / series | `POST /bookings/{id}/cancel` |
| Cancel one occurrence | `POST /bookings/{id}/occurrences/{occurrenceId}/cancel` |

### Example flow

```bash
# 1. onboard
curl -X POST localhost:8080/maids -H 'Content-Type: application/json' -d '{
  "name": "Asha", "phone": "9876543210", "locality": "HSR Layout", "gender": "FEMALE",
  "hourlyRates": {"CLEANING": 200, "COOKING": 300},
  "availability": [
    {"day": "MONDAY", "from": "08:00", "to": "13:00"},
    {"day": "MONDAY", "from": "16:00", "to": "20:00"},
    {"day": "SATURDAY", "from": "08:00", "to": "18:00"}
  ],
  "rating": 4.5
}'

# 2. search
curl 'localhost:8080/maids/search?locality=hsr%20layout&serviceType=COOKING&start=2026-11-09T09:00&durationMinutes=120'

# 3a. scheduled booking
curl -X POST localhost:8080/bookings -H 'Content-Type: application/json' -d '{
  "customerId": "cust-1", "maidId": "<maidId>", "serviceType": "COOKING",
  "type": "SCHEDULED", "durationMinutes": 120, "startAt": "2026-11-09T09:00"
}'

# 3b. recurring booking: every Monday at 9am for a month
curl -X POST localhost:8080/bookings -H 'Content-Type: application/json' -d '{
  "customerId": "cust-1", "maidId": "<maidId>", "serviceType": "CLEANING",
  "type": "RECURRING", "durationMinutes": 60,
  "recurrence": {"days": ["MONDAY"], "time": "09:00", "startDate": "2026-11-09", "endDate": "2026-12-07"}
}'

# 3c. instant booking: needs the maid to be working right now
curl -X POST localhost:8080/bookings -H 'Content-Type: application/json' -d '{
  "customerId": "cust-1", "maidId": "<maidId>", "serviceType": "CLEANING", "type": "INSTANT", "durationMinutes": 60
}'

# 4. pay (occurrenceId optional: defaults to the earliest unpaid one)
curl -X POST localhost:8080/bookings/<bookingId>/payments \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: 7f1c-001' \
  -d '{"method": "UPI", "instrument": "asha@okicici"}'

# 5. cancel
curl -X POST localhost:8080/bookings/<bookingId>/cancel
```

The mock gateway declines any instrument ending in `0000` (e.g. card `4111111111110000`), so you can try the failure path.

## Design

Packages are split by feature. Each one holds its domain objects, a repository interface plus
an in-memory implementation, a service and a controller:

```
common/        TimeSlot, exceptions + error mapping, KeyedLocks
maid/          Maid, AvailabilityWindow, onboarding
search/        MaidFilter + filters, MaidSearchService
booking/       Booking, Occurrence, BookingService, AvailabilityChecker
  planner/     SlotPlanner per booking type (Instant / Scheduled / Recurring)
payment/       PaymentProcessor per method, PaymentGateway (mocked), idempotency
cancellation/  CancellationPolicy, CancellationService
pricing/       PricingStrategy
```

**Booking = one or more occurrences.** Every booking type ends up as a list of time slots
(occurrences). Instant and scheduled bookings have one, recurring bookings have one per repetition.
Availability, payment, cancellation and refunds then only deal with occurrences and never need
to know the booking type. That is why the three types share one code path.

**Extension points** (add a class, nothing else changes):

| To add | Implement | Wiring |
|---|---|---|
| Booking type (e.g. "monthly") | `SlotPlanner` + enum value | collected by type in `BookingService` |
| Payment method (e.g. net banking) | `PaymentProcessor` + enum value | collected by method in `PaymentService` |
| Search filter (e.g. language) | `MaidFilter` bean | all filters applied in `MaidSearchService` |
| Refund rules | `CancellationPolicy` bean | |
| Dynamic / surge pricing | `PricingStrategy` bean | |
| Real DB | `MaidRepository` / `BookingRepository` / `PaymentRepository` | |

**State machine** (per occurrence; the booking status is derived from its occurrences):

```
PENDING_PAYMENT --pay ok--> CONFIRMED
PENDING_PAYMENT --cancel--> CANCELLED (no refund)
CONFIRMED       --cancel--> CANCELLED (refund per policy)
```
Paying twice, paying or cancelling a cancelled occurrence, and acting on an occurrence that has
already ended all return `409`.

**Availability.** A slot is free if it fits entirely inside one of the maid's weekly windows and
doesn't overlap any of her non-cancelled occurrences. Slots are half-open, so 10-11 and 11-12 don't
clash. Search and booking use the same `AvailabilityChecker`. Cancelling just marks the occurrence
`CANCELLED`, which is enough to make the slot discoverable and bookable again.

**Concurrency.**
- Booking: the check-then-save runs under a per-maid lock. Two customers racing for the same
  slot get exactly one booking (covered by a 20-thread test). Different maids never block each other.
- Pay and cancel: both run under a per-booking lock, so "pay while cancelling" or "two parallel
  payments" can't double-charge.
- Recurring series: all-or-nothing. If any occurrence clashes, nothing is booked and the error
  lists the clashing dates.

**Payments.**
- Payment is per occurrence. For a recurring series the customer pays each visit (or pays them
  one after another). A cancelled visit refunds exactly its own payment.
- The `Idempotency-Key` header is required. Replaying the same key returns the original result
  without charging again. Reusing a key for a different booking or method is a `409`.
- A failed payment leaves the occurrence `PENDING_PAYMENT`. Retry with a new key.

**Refund policy** (`TimeBasedCancellationPolicy`): 100% if cancelled 24h or more before start,
50% if 2-24h before, 0% after that. Unpaid occurrences are simply released.

## Assumptions

- Single city / time zone (Asia/Kolkata). Times are local date-times.
- No auth: `customerId` is taken as given.
- A maid's phone number is her identity. Onboarding the same phone twice is rejected.
- The services a maid offers are the keys of her `hourlyRates`, so skills and prices can't drift apart.
- Price = hourly rate x duration. A booking lasts 30 min to 8 h and must fit inside a single availability window.
- Bookings (including the end of a recurring series) can be at most 90 days ahead. Recurring series must have an end date.
- An instant booking starts at the current minute. Instant bookings can be paid or cancelled until the slot ends.
- Rating is optional at onboarding (new maids are unrated). Unrated maids are excluded by a `minRating` filter and listed last.
- A customer may book two different maids for the same time (e.g. a cook and a cleaner).

## With more time

- Expire unpaid holds (e.g. after 15 min) so abandoned bookings stop blocking the maid. Today a pending occurrence holds its slot until it is cancelled.
- Persist to a DB and replace in-memory locks with row locks / optimistic versioning, or an exclusion constraint on (maid, time range).
- Index occurrences by maid and date instead of scanning, for search at scale.
- Maid leave / one-off unavailability, travel buffer between jobs, and occurrence `COMPLETED` / no-show states.
- Real gateway integration with webhooks, plus async refunds with retries.
