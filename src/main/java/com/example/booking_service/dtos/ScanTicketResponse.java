package com.example.booking_service.dtos;

import com.example.booking_service.model.TicketStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class ScanTicketResponse {
    private UUID ticketId;
    private TicketStatus status;
}
