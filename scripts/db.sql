-- Booking / Ticket Service - PostgreSQL DDL
-- Owns:
--   - bookings
--   - booking_items
--   - tickets
--
-- Notes:
-- 1) payment_id, event_id, user_id, lock_id, event_seat_id, section_id are external references
--    from other services, so no foreign keys are added for them.
-- 2) booking creation should be idempotent on payment_id.
-- 3) One booking can contain multiple seats/tickets.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

select current_database(); 
SET search_path TO booking_db;

-- ---------------------------------------------------------
-- 1) bookings
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookings (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  payment_id          UUID NOT NULL,   -- external ref from payment-service
  user_id             UUID NOT NULL,   -- external ref from user-service
  event_id            UUID NOT NULL,   -- external ref from event-service
  lock_id             UUID NOT NULL,   -- external ref from seat-allocation-service

  currency            VARCHAR(3) NOT NULL,
  total_amount_minor  BIGINT NOT NULL CHECK (total_amount_minor > 0),

  status              VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',

  booked_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_bookings_payment_id UNIQUE (payment_id),
  CONSTRAINT chk_bookings_status CHECK (
    status IN ('CONFIRMED', 'CANCELLED', 'PARTIALLY_CANCELLED', 'REFUNDED')
  ),
  CONSTRAINT chk_bookings_currency_len CHECK (length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_bookings_user_booked_at
  ON bookings(user_id, booked_at DESC);

CREATE INDEX IF NOT EXISTS idx_bookings_event_id
  ON bookings(event_id);

CREATE INDEX IF NOT EXISTS idx_bookings_status
  ON bookings(status);

CREATE INDEX IF NOT EXISTS idx_bookings_lock_id
  ON bookings(lock_id);

-- ---------------------------------------------------------
-- 2) booking_items
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS booking_items (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  booking_id          UUID NOT NULL,
  event_seat_id       UUID NOT NULL,   -- external ref from seat-allocation-service
  section_id          UUID NOT NULL,   -- external ref from event-service
  price_minor         BIGINT NOT NULL CHECK (price_minor > 0),

  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_booking_items_booking
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
    ON DELETE CASCADE,

  CONSTRAINT uq_booking_items_booking_event_seat UNIQUE (booking_id, event_seat_id)
);

CREATE INDEX IF NOT EXISTS idx_booking_items_booking_id
  ON booking_items(booking_id);

CREATE INDEX IF NOT EXISTS idx_booking_items_event_seat_id
  ON booking_items(event_seat_id);

CREATE INDEX IF NOT EXISTS idx_booking_items_section_id
  ON booking_items(section_id);

-- ---------------------------------------------------------
-- 3) tickets
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS tickets (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  booking_id          UUID NOT NULL,
  booking_item_id     UUID NOT NULL,

  event_id            UUID NOT NULL,   -- external ref from event-service
  user_id             UUID NOT NULL,   -- external ref from user-service

  ticket_number       VARCHAR(64) NOT NULL,
  ticket_code         VARCHAR(128) NOT NULL,  -- used in QR / scan flow

  status              VARCHAR(30) NOT NULL DEFAULT 'ISSUED',

  issued_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  checked_in_at       TIMESTAMPTZ NULL,
  cancelled_at        TIMESTAMPTZ NULL,

  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_tickets_booking
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_tickets_booking_item
    FOREIGN KEY (booking_item_id) REFERENCES booking_items(id)
    ON DELETE CASCADE,

  CONSTRAINT uq_tickets_ticket_number UNIQUE (ticket_number),
  CONSTRAINT uq_tickets_ticket_code UNIQUE (ticket_code),
  CONSTRAINT uq_tickets_booking_item UNIQUE (booking_item_id),

  CONSTRAINT chk_tickets_status CHECK (
    status IN ('ISSUED', 'CANCELLED', 'USED', 'INVALIDATED')
  ),

  CONSTRAINT chk_ticket_time_consistency CHECK (
    (status = 'USED' AND checked_in_at IS NOT NULL)
    OR
    (status <> 'USED')
  )
);

CREATE INDEX IF NOT EXISTS idx_tickets_booking_id
  ON tickets(booking_id);

CREATE INDEX IF NOT EXISTS idx_tickets_user_id
  ON tickets(user_id);

CREATE INDEX IF NOT EXISTS idx_tickets_event_id
  ON tickets(event_id);

CREATE INDEX IF NOT EXISTS idx_tickets_status
  ON tickets(status);

CREATE INDEX IF NOT EXISTS idx_tickets_ticket_code
  ON tickets(ticket_code);

-- ---------------------------------------------------------
-- 4) Optional: fulfillment idempotency / retry support
-- ---------------------------------------------------------
-- Useful if payment-service retries finalize-booking call.
CREATE TABLE IF NOT EXISTS booking_fulfillment_requests (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  payment_id          UUID NOT NULL,
  request_hash        VARCHAR(128) NOT NULL,
  booking_id          UUID NOT NULL,

  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_booking_fulfillment_payment UNIQUE (payment_id),
  CONSTRAINT fk_booking_fulfillment_booking
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
    ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_booking_fulfillment_booking_id
  ON booking_fulfillment_requests(booking_id);

COMMIT;