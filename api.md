# Booking Ticket Service API

## Public APIs

### Get Booking History

GET /api/v1/bookings

Headers

Authorization: Bearer <JWT>

Response

`{
  "bookings": [
    {
      "bookingId": "uuid",
      "eventId": "uuid",
      "status": "CONFIRMED",
      "currency": "INR",
      "totalAmountMinor": 350000
    }
  ]
}`

---

### Get Booking Detail

GET /api/v1/bookings/{bookingId}

Response

`{
  "bookingId": "uuid",
  "eventId": "uuid",
  "status": "CONFIRMED",
  "items": [
    {
      "bookingItemId": "uuid",
      "eventSeatId": "uuid",
      "sectionId": "uuid",
      "priceMinor": 150000
    }
  ]
}`

---

### Get Tickets for Booking

GET /api/v1/bookings/{bookingId}/tickets

Response

`[
  {
    "ticketId": "uuid",
    "ticketNumber": "TKT-2026-000001",
    "ticketCode": "ABC123XYZ",
    "status": "ISSUED"
  }
]`

---

### Get Ticket Detail

GET /api/v1/tickets/{ticketId}

Response

`{
  "ticketId": "uuid",
  "ticketNumber": "TKT-2026-000001",
  "ticketCode": "ABC123XYZ",
  "status": "ISSUED"
}`

---

### Scan Ticket

POST /api/v1/tickets/scan

Request

`{
  "ticketCode": "ABC123XYZ"
}`

Response

`{
  "ticketId": "uuid",
  "status": "USED"
}`

---

## Internal APIs

### Finalize Booking

POST /internal/v1/bookings/finalize

Headers

`X-Internal-Auth: <shared-secret>`

Request

`{
  "paymentId": "uuid",
  "userId": "uuid",
  "eventId": "uuid",
  "lockId": "uuid",
  "currency": "INR",
  "totalAmountMinor": 350000,
  "seats": [
    {
      "eventSeatId": "uuid",
      "sectionId": "uuid",
      "priceMinor": 150000
    }
  ]
}`

Response

`{
  "bookingId": "uuid",
  "status": "CONFIRMED",
  "tickets": [
    {
      "ticketId": "uuid",
      "ticketNumber": "TKT-2026-000001"
    }
  ]
}`