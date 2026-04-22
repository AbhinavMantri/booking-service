package com.example.booking_service.integration;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentSnapshot {
    private UUID paymentId;
    private UUID eventId;
    private UUID lockId;
    private Long amountMinor;
    private String currency;
    private String status;
    private String providerKeyId;
    private String providerOrderId;
    private String providerPaymentId;
}
