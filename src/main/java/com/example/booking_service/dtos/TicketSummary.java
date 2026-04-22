package com.example.booking_service.dtos;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketSummary {
    private UUID ticketId;
    private String ticketNumber;
}
