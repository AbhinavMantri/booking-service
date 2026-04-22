package com.example.booking_service.dtos;

import java.util.UUID;

import com.example.booking_service.model.TicketStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketDetails {
    private UUID ticketId;
    private String ticketNumber;
    private String ticketCode;
    private TicketStatus ticketStatus;
}
