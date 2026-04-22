package com.example.booking_service.exceptions;

import java.util.UUID;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(UUID ticketId) {
        super("Ticket not found: " + ticketId);
    }

    public TicketNotFoundException(String ticketCode) {
        super("Ticket not found: " + ticketCode);
    }
}
