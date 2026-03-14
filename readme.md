# Booking / Ticket Service

## Overview
`booking-service` handles post-payment fulfillment for the ticketing platform.

After a successful payment and seat confirmation, this service creates:
- bookings
- booking items (one per seat)
- tickets

It also exposes APIs for retrieving bookings and validating tickets.

---

## Responsibilities

### Owned by this service
- create booking records
- create booking items per seat
- generate tickets
- provide booking history APIs
- provide ticket retrieval APIs
- support ticket scan/check-in flow

### Not owned by this service
- payment processing
- seat locking or seat inventory
- event/venue creation
- authentication (handled by user-service)

---

## Service Dependencies

### Upstream
- API Gateway
- user-service (JWT authentication)

### Internal Services
- payment-service
- seat-allocation-service
- event-service (optional read enrichment)

---

## Booking Flow

1. Customer locks seats using seat-allocation-service
2. Customer completes payment through payment-service
3. Payment provider webhook confirms payment
4. payment-service confirms seats with seat-allocation-service
5. payment-service calls booking-service to finalize booking
6. booking-service creates:
   - booking
   - booking items
   - tickets
7. Customer retrieves booking and tickets through this service

---

## Core Entities

### Booking

Represents a successful purchase transaction.

Fields:
- id
- paymentId
- userId
- eventId
- lockId
- currency
- totalAmountMinor
- status
- bookedAt

Booking Status:
- CONFIRMED
- CANCELLED
- PARTIALLY_CANCELLED
- REFUNDED

---

### BookingItem

Represents a seat within a booking.

Fields:
- id
- bookingId
- eventSeatId
- sectionId
- priceMinor

---

### Ticket

Represents the admission ticket for a seat.

Fields:
- id
- bookingId
- bookingItemId
- eventId
- userId
- ticketNumber
- ticketCode
- status
- issuedAt
- checkedInAt

Ticket Status:
- ISSUED
- USED
- CANCELLED
- INVALIDATED

---

## Idempotency

Booking creation must be idempotent.

Rules:
- unique constraint on `paymentId`
- if finalize booking is retried, return the existing booking

---

## Security

Public APIs
- secured via JWT authentication

Internal APIs
- accessible only by internal services
- protected via service-to-service authentication

---

## Project Structure

booking-service