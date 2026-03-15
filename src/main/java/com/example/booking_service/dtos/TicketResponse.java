package com.example.booking_service.dtos;

import com.example.booking_service.model.TicketStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class TicketResponse {
    private UUID ticketId;
    private String ticketNumber;
    private String ticketCode;
    private TicketStatus status;
}
