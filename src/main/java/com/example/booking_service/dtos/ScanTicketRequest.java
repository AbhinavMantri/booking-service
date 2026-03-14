package com.example.booking_service.dtos;

import jakarta.validation.constraints.NotBlank;

public record ScanTicketRequest(@NotBlank String ticketCode) {
}
