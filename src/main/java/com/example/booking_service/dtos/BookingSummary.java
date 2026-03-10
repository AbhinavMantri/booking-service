package com.example.booking_service.dtos;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BookingSummary {
    private UUID bookingId;
    private String status;
    private List<TicketSummary> tickets;
}
