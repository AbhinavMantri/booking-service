package com.example.booking_service.services;

import com.example.booking_service.dtos.ScanTicketRequest;
import com.example.booking_service.exceptions.TicketNotFoundException;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.model.TicketStatus;
import com.example.booking_service.repositories.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        MDC.put("requestId", "ticket-service-test");
    }

    @Test
    void getTicketsForBookingReturnsTickets() {
        UUID bookingId = UUID.randomUUID();
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());

        when(ticketRepository.findAllByBookingIdOrderByIssuedAtAsc(bookingId)).thenReturn(List.of(ticket));

        List<Ticket> result = ticketService.getTicketsForBooking(bookingId);

        assertThat(result).containsExactly(ticket);
    }

    @Test
    void getTicketReturnsTicketWhenFound() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        Ticket result = ticketService.getTicket(ticketId);

        assertThat(result).isSameAs(ticket);
    }

    @Test
    void getTicketThrowsWhenMissing() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicket(ticketId))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("Ticket not found: " + ticketId);
    }

    @Test
    void scanTicketMarksTicketUsedAndPersists() {
        ScanTicketRequest request = new ScanTicketRequest("CODE-123");
        Ticket ticket = new Ticket();
        UUID ticketId = UUID.randomUUID();
        ticket.setId(ticketId);
        ticket.setTicketCode("CODE-123");
        ticket.setStatus(TicketStatus.ISSUED);

        when(ticketRepository.findByTicketCode("CODE-123")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket result = ticketService.scanTicket(request);

        assertThat(result).isSameAs(ticket);
        assertThat(result.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(result.getCheckedInAt()).isNotNull();
        verify(ticketRepository).save(ticket);
    }

    @Test
    void scanTicketThrowsWhenCodeMissing() {
        ScanTicketRequest request = new ScanTicketRequest("MISSING");
        when(ticketRepository.findByTicketCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.scanTicket(request))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("Ticket not found: MISSING");
    }
}
