package com.example.booking_service.integration;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentInitiationPayload {
    private UUID userId;
    private UUID eventId;
    private UUID lockId;
    private Long amountMinor;
    private String currency;
    private String provider;
}
