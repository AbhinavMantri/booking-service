package com.example.booking_service.dtos;

import com.example.booking_service.dtos.common.ApiResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class FinalizeBookingTicketResponse extends ApiResponse {
   private TicketSummary ticketSummary;
}
