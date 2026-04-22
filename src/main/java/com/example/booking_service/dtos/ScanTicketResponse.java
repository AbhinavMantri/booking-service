package com.example.booking_service.dtos;

import com.example.booking_service.dtos.common.ApiResponse;
import com.example.booking_service.model.TicketStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
public class ScanTicketResponse extends ApiResponse {
    private UUID ticketId;
    private TicketStatus ticketStatus;
}
