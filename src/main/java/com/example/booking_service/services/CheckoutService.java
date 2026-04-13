package com.example.booking_service.services;

import com.example.booking_service.dtos.BookingRequest;
import com.example.booking_service.dtos.BookingSeatRequest;
import com.example.booking_service.dtos.CheckoutRequest;
import com.example.booking_service.dtos.CheckoutResponse;
import com.example.booking_service.dtos.ProcessPaymentOutcomeRequest;
import com.example.booking_service.dtos.ProcessPaymentOutcomeResponse;
import com.example.booking_service.dtos.common.ResponseStatus;
import com.example.booking_service.integration.LockSnapshot;
import com.example.booking_service.integration.PaymentGatewayClient;
import com.example.booking_service.integration.PaymentInitiationPayload;
import com.example.booking_service.integration.PaymentSnapshot;
import com.example.booking_service.integration.SeatAllocationGateway;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingStatus;
import com.example.booking_service.repositories.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {
    private final SeatAllocationGateway seatAllocationGateway;
    private final PaymentGatewayClient paymentGatewayClient;
    private final InternalBookingService internalBookingService;
    private final BookingRepository bookingRepository;

    public CheckoutResponse initiateCheckout(UUID userId, String idempotencyKey, CheckoutRequest request) {
        UUID bookingId = buildBookingId(userId, request.getEventId(), idempotencyKey);
        seatAllocationGateway.lockSeats(request.getEventId(), bookingId, userId, idempotencyKey, request.getSeatIds());
        LockSnapshot lockSnapshot = seatAllocationGateway.getLockDetails(bookingId);
        if (!request.getEventId().equals(lockSnapshot.getEventId())) {
            throw new IllegalStateException("Locked event does not match checkout event");
        }

        PaymentInitiationPayload paymentRequest = new PaymentInitiationPayload();
        paymentRequest.setUserId(userId);
        paymentRequest.setEventId(lockSnapshot.getEventId());
        paymentRequest.setLockId(bookingId);
        paymentRequest.setAmountMinor(lockSnapshot.getTotalAmountMinor());
        paymentRequest.setCurrency(lockSnapshot.getCurrency());
        paymentRequest.setProvider(request.getProvider().trim().toUpperCase(Locale.ROOT));
        PaymentSnapshot payment = paymentGatewayClient.initiatePayment(idempotencyKey, paymentRequest);

        CheckoutResponse response = new CheckoutResponse();
        response.setStatus(ResponseStatus.SUCCESS);
        response.setMessage("Checkout initiated successfully");
        response.setBookingId(bookingId);
        response.setPaymentId(payment.getPaymentId());
        response.setEventId(lockSnapshot.getEventId());
        response.setTotalAmountMinor(lockSnapshot.getTotalAmountMinor());
        response.setCurrency(lockSnapshot.getCurrency());
        response.setPaymentStatus(payment.getStatus());
        response.setProviderKeyId(payment.getProviderKeyId());
        response.setProviderOrderId(payment.getProviderOrderId());
        response.setSeats(toBookingSeats(lockSnapshot));
        return response;
    }

    public ProcessPaymentOutcomeResponse processPaymentOutcome(ProcessPaymentOutcomeRequest request) {
        PaymentOutcomeAction action = resolveAction(request.getPaymentStatus());
        ProcessPaymentOutcomeResponse response = new ProcessPaymentOutcomeResponse();
        response.setStatus(ResponseStatus.SUCCESS);
        response.setBookingId(request.getLockId());

        if (action == PaymentOutcomeAction.NO_OP) {
            response.setAction(PaymentOutcomeAction.NO_OP.name());
            response.setMessage("No booking action required for payment status " + request.getPaymentStatus());
            return response;
        }

        if (action == PaymentOutcomeAction.SUCCESS) {
            Booking existingBooking = bookingRepository.findById(request.getLockId()).orElse(null);
            if (existingBooking != null && BookingStatus.CONFIRMED.name().equals(existingBooking.getStatus())) {
                response.setAction("BOOKING_ALREADY_CONFIRMED");
                response.setMessage("Booking already confirmed");
                return response;
            }

            LockSnapshot lockSnapshot = seatAllocationGateway.getLockDetails(request.getLockId());
            List<UUID> seatIds = lockSnapshot.getSeats().stream().map(seat -> seat.getEventSeatId()).toList();
            seatAllocationGateway.confirmSeats(
                    request.getEventId(),
                    request.getLockId(),
                    request.getPaymentId(),
                    seatIds,
                    "confirm:" + request.getPaymentId()
            );

            BookingRequest finalizeRequest = new BookingRequest();
            finalizeRequest.setPaymentId(request.getPaymentId());
            finalizeRequest.setUserId(request.getUserId());
            finalizeRequest.setEventId(request.getEventId());
            finalizeRequest.setLockId(request.getLockId());
            finalizeRequest.setCurrency(request.getCurrency());
            finalizeRequest.setTotalAmountMinor(request.getTotalAmountMinor());
            finalizeRequest.setSeats(toBookingSeats(lockSnapshot));
            Booking booking = internalBookingService.finalizeBooking(finalizeRequest);
            response.setBookingId(booking.getId());
            response.setAction("BOOKING_CONFIRMED");
            response.setMessage("Booking confirmed successfully");
            return response;
        }

        Booking existingBooking = bookingRepository.findById(request.getLockId()).orElse(null);
        if (existingBooking != null && BookingStatus.CONFIRMED.name().equals(existingBooking.getStatus())) {
            response.setAction("BOOKING_ALREADY_CONFIRMED");
            response.setMessage("Booking already confirmed");
            return response;
        }

        LockSnapshot lockSnapshot = seatAllocationGateway.getLockDetailsOrNull(request.getLockId());
        if (lockSnapshot == null) {
            response.setAction("LOCK_ALREADY_CLEARED");
            response.setMessage("No active seat lock remains for this checkout");
            return response;
        }

        List<UUID> seatIds = lockSnapshot.getSeats().stream().map(seat -> seat.getEventSeatId()).toList();
        seatAllocationGateway.releaseSeats(
                request.getEventId(),
                request.getLockId(),
                seatIds,
                action,
                "release:" + request.getPaymentId() + ":" + action.name()
        );
        response.setAction("SEATS_RELEASED");
        response.setMessage("Seat lock released successfully");
        return response;
    }

    private List<BookingSeatRequest> toBookingSeats(LockSnapshot lockSnapshot) {
        return lockSnapshot.getSeats().stream()
                .map(seat -> {
                    BookingSeatRequest bookingSeat = new BookingSeatRequest();
                    bookingSeat.setEventSeatId(seat.getEventSeatId());
                    bookingSeat.setSectionId(seat.getSectionId());
                    bookingSeat.setPriceMinor(seat.getPriceCents() == null ? null : seat.getPriceCents().longValue());
                    return bookingSeat;
                })
                .toList();
    }

    private UUID buildBookingId(UUID userId, UUID eventId, String idempotencyKey) {
        String raw = userId + ":" + eventId + ":" + idempotencyKey;
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }

    private PaymentOutcomeAction resolveAction(String paymentStatus) {
        if (paymentStatus == null) {
            return PaymentOutcomeAction.NO_OP;
        }
        String normalized = paymentStatus.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SUCCESS", "SUCCESS_CONFIRMATION_FAILED" -> PaymentOutcomeAction.SUCCESS;
            case "FAILED", "REFUNDED" -> PaymentOutcomeAction.FAILED;
            case "CANCELLED" -> PaymentOutcomeAction.CANCELLED;
            default -> PaymentOutcomeAction.NO_OP;
        };
    }
}
