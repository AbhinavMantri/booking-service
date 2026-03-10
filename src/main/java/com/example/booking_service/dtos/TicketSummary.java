package com.example.booking_service.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class TicketSummary {
    private UUID ticketId;
    private String ticketNumber;
}
