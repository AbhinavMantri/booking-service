# Booking Service

`booking-service` is the booking fulfillment and ticket issuance microservice for the Ticket Master capstone application.

It is responsible for turning a successful checkout into:
- a persisted booking
- one booking item per confirmed seat
- one issued ticket per booking item

It also exposes customer-facing APIs to fetch bookings and tickets, and an internal API to finalize a booking after payment and seat confirmation.

## Scope

### Owned by this service
- Persist bookings, booking items, and tickets
- Expose booking history and booking detail APIs
- Expose ticket detail and ticket scan APIs
- Provide idempotent booking finalization based on `paymentId` and `lockId`

### Explicitly out of scope
- Seat search, reservation, and lock management
- Payment orchestration and payment status tracking
- Event, show, venue, section, and seat master data
- Customer identity lifecycle

## Where It Fits In The Capstone

Typical purchase flow:

1. Customer selects seats in a seat inventory or seat-allocation service.
2. Payment is processed by a payment service.
3. Payment success is confirmed.
4. The payment or orchestration layer calls `booking-service` internal finalize API.
5. `booking-service` creates the booking, booking items, and tickets.
6. The customer later uses public APIs to view bookings and tickets.
7. Entry or operations staff can mark a ticket as used via the scan endpoint.

## Current Capabilities

- `POST /internal/bookings/finalize`
  Creates a confirmed booking and issues tickets.
- `GET /bookings`
  Returns bookings for the authenticated user.
- `GET /bookings/{bookingId}`
  Returns a booking and its items.
- `GET /bookings/{bookingId}/tickets`
  Returns tickets for a booking.
- `GET /tickets/{ticketId}`
  Returns ticket details.
- `POST /tickets/scan`
  Marks a ticket as `USED` using `ticketCode`.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web
- Spring Data JPA
- PostgreSQL
- Lombok
- JUnit + Mockito + H2 for tests

## Project Structure

```text
src/main/java/com/example/booking_service
|- controllers        # REST entry points
|- services           # business logic
|- repositories       # JPA repositories
|- model              # JPA entities and enums
|- dtos               # request/response payloads
|- logging            # request correlation and JWT auth filter

scripts/db.sql        # PostgreSQL DDL
src/main/resources    # profile-specific configuration
```

## Data Model

### Booking
- `id`
- `paymentId`
- `userId`
- `eventId`
- `lockId`
- `currency`
- `totalAmountMinor`
- `status`
- `bookedAt`

Statuses currently supported:
- `CONFIRMED`
- `CANCELLED`
- `PARTIALLY_CANCELLED`
- `REFUNDED`

### Booking Item
- `id`
- `bookingId`
- `eventSeatId`
- `sectionId`
- `priceMinor`

### Ticket
- `id`
- `bookingId`
- `bookingItemId`
- `eventId`
- `userId`
- `ticketNumber`
- `ticketCode`
- `status`
- `issuedAt`
- `checkedInAt`

Statuses currently supported:
- `ISSUED`
- `USED`
- `CANCELLED`
- `INVALIDATED`

## API Base Path

Configured in [application.properties](/d:/Scaler-Projects/booking-service/src/main/resources/application.properties:1):

- `api.prefix=/booking-service/v1`
- `server.servlet.context-path=${api.prefix}`

All endpoints are therefore served under:

`/booking-service/v1`

## Security Model

### Public APIs
Implemented through [PublicApiAuthenticationFilter](/d:/Scaler-Projects/booking-service/src/main/java/com/example/booking_service/logging/PublicApiAuthenticationFilter.java:1):
- Require `Authorization: Bearer <jwt>`
- Expect a `userId` claim in the JWT
- Reject missing or invalid tokens with `401`

### Internal APIs
Current implementation note:
- The repo has config placeholders for internal service tokens in [application-dev.properties](/d:/Scaler-Projects/booking-service/src/main/resources/application-dev.properties:1)
- No internal authentication filter or header validation is currently implemented
- The internal finalize endpoint is therefore unsecured at the application layer right now

### Request Correlation
Implemented through [RequestCorrelationFilter](/d:/Scaler-Projects/booking-service/src/main/java/com/example/booking_service/logging/RequestCorrelationFilter.java:1):
- Accepts optional `X-Request-Id`
- Generates one if absent
- Echoes `X-Request-Id` in the response

## Idempotency Behavior

Finalization is idempotent by business key:

- If a confirmed booking already exists for the same `paymentId`, the existing booking is returned.
- If a confirmed booking already exists for the same `lockId`, the existing booking is returned.
- If either key maps to a non-confirmed booking, the service returns `409 Conflict`.

This behavior is implemented in [InternalBookingService](/d:/Scaler-Projects/booking-service/src/main/java/com/example/booking_service/services/InternalBookingService.java:1).

## Database

The schema is defined in [scripts/db.sql](/d:/Scaler-Projects/booking-service/scripts/db.sql:1).

Notable constraints:
- Unique `payment_id` on `bookings`
- Unique `(booking_id, event_seat_id)` on `booking_items`
- Unique `ticket_number`, `ticket_code`, and `booking_item_id` on `tickets`
- Foreign keys from `booking_items` to `bookings`
- Foreign keys from `tickets` to `bookings` and `booking_items`

Current runtime expectation:
- `spring.jpa.hibernate.ddl-auto=validate`
- Schema should be created ahead of time

## Local Setup

### Prerequisites
- Java 21
- PostgreSQL running locally
- Database named `booking_db`

### Recommended local profile

Use the `dev` profile:

```powershell
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### Useful environment variables

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_PROFILES_ACTIVE`
- `SECURITY_JWT_SECRET` if externalized through your runtime environment

Default datasource values are defined in [application.properties](/d:/Scaler-Projects/booking-service/src/main/resources/application.properties:1) and overridden in profile files.

## Testing

Run tests with:

```powershell
./mvnw.cmd test
```

The repository already includes controller and service tests for:
- booking retrieval
- ticket retrieval
- ticket scanning
- finalize booking idempotency and conflict cases

## MVP Gaps To Call Out

These are the main gaps I would call out for an MVP review based on current implementation:

1. Internal endpoint authentication is missing.
   The finalize API should not be exposed without service-to-service auth.

2. Ownership checks are incomplete on public read APIs.
   `GET /bookings/{bookingId}`, `GET /bookings/{bookingId}/tickets`, and `GET /tickets/{ticketId}` do not verify that the authenticated user owns the requested resource.

3. Scan flow lacks state validation.
   Scanning an already used, cancelled, or invalidated ticket currently just overwrites status to `USED`.

4. Request validation is minimal.
   `@Valid` is present on the finalize request, but `BookingRequest` and `BookingSeatRequest` do not currently enforce field-level constraints such as non-null UUIDs, currency format, positive amounts, or non-empty seats.

5. No global error contract exists.
   Some APIs return `ApiResponse`, some return bare `404`, and validation failures are not documented through a centralized exception handler.

6. No OpenAPI or Swagger contract is generated.
   For a capstone microservice, an executable API spec would reduce integration friction.

7. Ticket response from finalize has a mapping issue.
   `FinalizeBookingTicketResponse` currently sets `ticketId` from `bookingId` instead of the actual ticket id in [InternalBookingController](/d:/Scaler-Projects/booking-service/src/main/java/com/example/booking_service/controllers/InternalBookingController.java:1).

## Future Considerations

- Add booking cancellation and refund workflows
- Add ticket invalidation and reissue flows
- Add event enrichment or composition with event-service
- Add QR code generation or signed barcode payloads
- Add audit trail for scan attempts and fulfillment retries
- Add outbox/event publishing for booking-confirmed and ticket-issued events
- Add optimistic concurrency or deduplicated fulfillment request records
- Add rate limiting and stronger abuse controls on scan endpoints
- Add observability with metrics, traces, and alerting
- Add API versioning strategy and OpenAPI publishing in CI

## Suggested Next MVP Additions

If the goal is a solid capstone MVP, I would prioritize these next:

1. Secure the internal finalize endpoint.
2. Enforce user ownership on all public booking and ticket reads.
3. Add validation rules on booking finalize payloads.
4. Make ticket scan idempotent and reject invalid state transitions.
5. Standardize error responses with a global exception handler.

## Related Documentation

- API reference: [api.md](/d:/Scaler-Projects/booking-service/api.md:1)
- Schema: [scripts/db.sql](/d:/Scaler-Projects/booking-service/scripts/db.sql:1)
