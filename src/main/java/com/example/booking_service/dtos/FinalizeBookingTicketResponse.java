package com.example.booking_service.dtos;

import com.example.booking_service.dtos.common.ApiResponse;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class FinalizeBookingTicketResponse extends ApiResponse {
    private UUID ticketId;
    private String ticketNumber;
}
