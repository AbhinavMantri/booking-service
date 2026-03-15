package com.example.booking_service.dtos;

import com.example.booking_service.dtos.common.ApiResponse;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class BookingTicketResponse extends ApiResponse {
    private List<TicketDetails> tickets;
}
