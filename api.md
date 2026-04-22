# Booking Service API

This document describes the current HTTP contract exposed by `booking-service` for the Ticket Master capstone application.

Base URL:

`/booking-service/v1`

## Conventions

### Headers

Public APIs:
- `Authorization: Bearer <jwt>` is required
- `X-Request-Id: <value>` is optional and echoed back in the response

Internal APIs:
- `X-Request-Id: <value>` is optional and echoed back in the response
- No internal auth header is currently enforced by code

### Response Status Field

Where `ApiResponse` is used, the service returns:
- `SUCCESS`
- `FAILURE`

### Common Error Cases

- `401 Unauthorized` for missing or invalid bearer token on public APIs
- `404 Not Found` for unknown booking or ticket in some endpoints
- `409 Conflict` when finalize booking detects conflicting state

## Public APIs

## GET `/bookings`

Returns all bookings for the authenticated user, ordered by newest booking first.

### Headers

```http
Authorization: Bearer <jwt>
X-Request-Id: client-generated-id
```

### Success Response

`200 OK`

```json
{
  "message": "Bookings retrieved successfully",
  "status": "SUCCESS",
  "bookings": [
    {
      "bookingId": "3e818308-05b6-43db-95fc-d3945c93c29f",
      "eventId": "8df23655-ef66-4ca8-a80a-f877487aa487",
      "status": "CONFIRMED",
      "currency": "USD",
      "totalAmountMinor": 1000
    }
  ]
}
```

### Error Response

`401 Unauthorized`

```json
{
  "message": "Missing or invalid Authorization header",
  "status": "FAILURE"
}
```

## GET `/bookings/{bookingId}`

Returns a booking and its booking items.

Current implementation note:
- This endpoint requires a valid JWT
- It does not verify that the booking belongs to the authenticated user
- Response does not use the common `ApiResponse` wrapper

### Path Params

- `bookingId` UUID

### Success Response

`200 OK`

```json
{
  "bookingId": "6fb7c628-6577-4e74-a07f-eb6027e37780",
  "eventId": "6309ee82-cfdb-4279-bc11-5135b4d45ce8",
  "status": "CONFIRMED",
  "items": [
    {
      "bookingItemId": "89dfd238-309a-4946-b4ea-4e1a41f29788",
      "eventSeatId": "3f21c5e5-56c8-49fc-9c5c-aee84c29c9ee",
      "sectionId": "6a48ef05-5ff2-44e2-b8db-1163f75d44db",
      "priceMinor": 1800
    }
  ]
}
```

### Error Response

`404 Not Found`

This endpoint currently returns an empty body on booking miss.

## GET `/bookings/{bookingId}/tickets`

Returns tickets issued for a booking, ordered by issue time ascending.

### Path Params

- `bookingId` UUID

### Success Response

`200 OK`

```json
{
  "message": "Tickets retrieved successfully",
  "status": "SUCCESS",
  "tickets": [
    {
      "ticketId": "0fe25560-5474-4e16-a89f-d50e72f1322c",
      "ticketNumber": "TKT-2026-ABC12345",
      "ticketCode": "F1C4B8B755CE4B6AA18A5FB9A7DB7D93",
      "ticketStatus": "ISSUED"
    }
  ]
}
```

### Error Response

`404 Not Found`

```json
{
  "message": "Booking not found: 6fb7c628-6577-4e74-a07f-eb6027e37780",
  "status": "FAILURE"
}
```

## GET `/tickets/{ticketId}`

Returns ticket details by ticket id.

Current implementation note:
- This endpoint requires a valid JWT
- It does not verify that the ticket belongs to the authenticated user

### Path Params

- `ticketId` UUID

### Success Response

`200 OK`

```json
{
  "message": "Ticket reterieved successfully",
  "status": "SUCCESS",
  "ticketDetails": {
    "ticketId": "1ec17a99-e3cf-4ac6-a0e6-3ab3c699cbab",
    "ticketNumber": "TKT-2026-ABC12345",
    "ticketCode": "F1C4B8B755CE4B6AA18A5FB9A7DB7D93",
    "ticketStatus": "ISSUED"
  }
}
```

### Error Response

`404 Not Found`

```json
{
  "message": "Ticket not found: 1ec17a99-e3cf-4ac6-a0e6-3ab3c699cbab",
  "status": "FAILURE"
}
```

## POST `/tickets/scan`

Marks a ticket as used by `ticketCode`.

Current implementation note:
- This endpoint is protected by the same public JWT filter as customer APIs
- It currently does not validate ticket state transitions and will overwrite status to `USED`

### Request Body

```json
{
  "ticketCode": "F1C4B8B755CE4B6AA18A5FB9A7DB7D93"
}
```

### Success Response

`200 OK`

```json
{
  "message": "Ticket scanned successfully",
  "status": "SUCCESS",
  "ticketId": "0fe25560-5474-4e16-a89f-d50e72f1322c",
  "ticketStatus": "USED"
}
```

### Error Response

`404 Not Found`

```json
{
  "message": "Ticket not found: MISSING",
  "status": "FAILURE"
}
```

## Internal APIs

## POST `/internal/bookings/finalize`

Creates or reuses a confirmed booking and issues one ticket per seat.

This is the service-to-service entry point expected to be called after payment success and seat confirmation.

### Request Body

```json
{
  "paymentId": "11111111-1111-1111-1111-111111111111",
  "userId": "22222222-2222-2222-2222-222222222222",
  "eventId": "33333333-3333-3333-3333-333333333333",
  "lockId": "44444444-4444-4444-4444-444444444444",
  "currency": "USD",
  "totalAmountMinor": 1000,
  "seats": [
    {
      "eventSeatId": "55555555-5555-5555-5555-555555555555",
      "sectionId": "66666666-6666-6666-6666-666666666666",
      "priceMinor": 1000
    }
  ]
}
```

### Success Response

`200 OK`

```json
{
  "bookingId": "77777777-7777-7777-7777-777777777777",
  "status": "SUCCESS",
  "tickets": [
    {
      "ticketSummary": {
        "ticketId": "CURRENT_IMPLEMENTATION_RETURNS_BOOKING_ID_HERE",
        "ticketNumber": "TKT-2026-ABC12345"
      }
    }
  ]
}
```

### Conflict Response

`409 Conflict`

```json
{
  "message": "Payment already exists for a non-confirmed booking",
  "status": "FAILURE"
}
```

### Current Behavior Notes

- If a confirmed booking already exists for `paymentId`, the service returns the existing booking.
- If a confirmed booking already exists for `lockId`, the service returns the existing booking.
- If an existing booking for those keys is not `CONFIRMED`, the service returns `409 Conflict`.
- `seats` may be omitted or null in the current code path, which would create a booking without booking items or tickets.
- The response payload currently nests tickets as `ticketSummary`.
- The current controller maps `ticketSummary.ticketId` from `bookingId` instead of the actual ticket id.

## Authentication Notes

### Public JWT Expectations

The JWT validator currently expects:
- HMAC SHA-256 signature
- issuer claim `iss` matching configured issuer
- `exp` claim present and not expired
- `userId` claim present

Config is handled in [JWTService](/d:/Scaler-Projects/booking-service/src/main/java/com/example/booking_service/services/JWTService.java:1).

### Internal Auth Gap

Although `application-dev.properties` defines:

```properties
internal.api.service-tokens.inventory-service=seats-strong-dev-token
```

there is no filter or controller validation enforcing internal service authentication yet.

## Recommended API Improvements

For a stronger MVP contract, I would add:

1. A dedicated internal auth filter for `/internal/**`.
2. Resource ownership enforcement on public read APIs.
3. Bean validation on finalize booking request fields.
4. A standard error envelope for every non-2xx response.
5. Explicit scan failure responses for already used, cancelled, or invalid tickets.
6. OpenAPI generation and published examples for all endpoints.
