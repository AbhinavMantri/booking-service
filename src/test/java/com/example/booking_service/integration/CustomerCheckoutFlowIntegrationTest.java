package com.example.booking_service.integration;

import com.example.booking_service.dtos.CheckoutRequest;
import com.example.booking_service.dtos.ProcessPaymentOutcomeRequest;
import com.example.booking_service.model.Booking;
import com.example.booking_service.model.BookingStatus;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.model.TicketStatus;
import com.example.booking_service.repositories.BookingItemRepository;
import com.example.booking_service.repositories.BookingRepository;
import com.example.booking_service.repositories.TicketRepository;
import com.example.booking_service.services.BookingService;
import com.example.booking_service.services.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:customer-checkout-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class CustomerCheckoutFlowIntegrationTest {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @MockitoBean
    private SeatAllocationGateway seatAllocationGateway;

    @MockitoBean
    private PaymentGatewayClient paymentGatewayClient;

    @BeforeEach
    void cleanDatabase() {
        ticketRepository.deleteAll();
        bookingItemRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void customerCheckoutPaymentSuccessCreatesBookingAndIssuedTickets() {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID seatOne = UUID.randomUUID();
        UUID seatTwo = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String idempotencyKey = "checkout-flow-1";
        UUID bookingId = deterministicBookingId(userId, eventId, idempotencyKey);

        LockSnapshot lockSnapshot = lockSnapshot(bookingId, eventId, sectionId, seatOne, seatTwo);
        when(seatAllocationGateway.getLockDetails(bookingId)).thenReturn(lockSnapshot);

        PaymentSnapshot paymentSnapshot = new PaymentSnapshot();
        paymentSnapshot.setPaymentId(paymentId);
        paymentSnapshot.setEventId(eventId);
        paymentSnapshot.setLockId(bookingId);
        paymentSnapshot.setAmountMinor(3000L);
        paymentSnapshot.setCurrency("INR");
        paymentSnapshot.setStatus("PENDING");
        paymentSnapshot.setProviderKeyId("rzp_test_customer_flow");
        paymentSnapshot.setProviderOrderId("order_customer_flow");
        when(paymentGatewayClient.initiatePayment(eq(idempotencyKey), org.mockito.ArgumentMatchers.any()))
                .thenReturn(paymentSnapshot);

        CheckoutRequest checkoutRequest = new CheckoutRequest();
        checkoutRequest.setEventId(eventId);
        checkoutRequest.setProvider("RAZORPAY");
        checkoutRequest.setSeatIds(List.of(seatOne, seatTwo));

        var checkoutResponse = checkoutService.initiateCheckout(userId, idempotencyKey, checkoutRequest);

        assertThat(checkoutResponse.getStatus().name()).isEqualTo("SUCCESS");
        assertThat(checkoutResponse.getBookingId()).isEqualTo(bookingId);
        assertThat(checkoutResponse.getPaymentId()).isEqualTo(paymentId);
        assertThat(checkoutResponse.getProviderOrderId()).isEqualTo("order_customer_flow");

        ProcessPaymentOutcomeRequest outcomeRequest = new ProcessPaymentOutcomeRequest();
        outcomeRequest.setPaymentId(paymentId);
        outcomeRequest.setUserId(userId);
        outcomeRequest.setEventId(eventId);
        outcomeRequest.setLockId(bookingId);
        outcomeRequest.setTotalAmountMinor(3000L);
        outcomeRequest.setCurrency("INR");
        outcomeRequest.setPaymentStatus("SUCCESS");

        var outcomeResponse = checkoutService.processPaymentOutcome(outcomeRequest);

        assertThat(outcomeResponse.getAction()).isEqualTo("BOOKING_CONFIRMED");
        assertThat(outcomeResponse.getBookingId()).isNotEqualTo(bookingId);
        Booking booking = bookingRepository.findById(outcomeResponse.getBookingId()).orElseThrow();
        assertThat(booking.getUserId()).isEqualTo(userId);
        assertThat(booking.getPaymentId()).isEqualTo(paymentId);
        assertThat(booking.getLockId()).isEqualTo(bookingId);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED.name());
        assertThat(booking.getTotalAmountMinor()).isEqualTo(3000L);

        assertThat(bookingItemRepository.findAllByBookingIdOrderByIdAsc(booking.getId())).hasSize(2);
        List<Ticket> tickets = bookingService.getTicketsForBooking(booking.getId());
        assertThat(tickets).hasSize(2);
        assertThat(tickets).allSatisfy(ticket -> {
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ISSUED);
            assertThat(ticket.getTicketNumber()).startsWith("TKT-");
            assertThat(ticket.getTicketCode()).isNotBlank();
        });

        verify(seatAllocationGateway).lockSeats(eventId, bookingId, userId, idempotencyKey, List.of(seatOne, seatTwo));
        verify(seatAllocationGateway).confirmSeats(
                eventId,
                bookingId,
                paymentId,
                List.of(seatOne, seatTwo),
                "confirm:" + paymentId
        );
    }

    private UUID deterministicBookingId(UUID userId, UUID eventId, String idempotencyKey) {
        String raw = userId + ":" + eventId + ":" + idempotencyKey;
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }

    private LockSnapshot lockSnapshot(UUID bookingId, UUID eventId, UUID sectionId, UUID seatOne, UUID seatTwo) {
        LockedSeatSnapshot firstSeat = new LockedSeatSnapshot();
        firstSeat.setEventSeatId(seatOne);
        firstSeat.setSectionId(sectionId);
        firstSeat.setPriceCents(1500);

        LockedSeatSnapshot secondSeat = new LockedSeatSnapshot();
        secondSeat.setEventSeatId(seatTwo);
        secondSeat.setSectionId(sectionId);
        secondSeat.setPriceCents(1500);

        LockSnapshot lockSnapshot = new LockSnapshot();
        lockSnapshot.setBookingId(bookingId);
        lockSnapshot.setEventId(eventId);
        lockSnapshot.setCurrency("INR");
        lockSnapshot.setTotalAmountMinor(3000L);
        lockSnapshot.setSeats(List.of(firstSeat, secondSeat));
        return lockSnapshot;
    }
}
