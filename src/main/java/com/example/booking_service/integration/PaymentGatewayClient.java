package com.example.booking_service.integration;

public interface PaymentGatewayClient {
    PaymentSnapshot initiatePayment(String idempotencyKey, PaymentInitiationPayload request);
}
