package com.example.booking_service.services;

import com.example.booking_service.dtos.ScanTicketRequest;
import com.example.booking_service.dtos.ScanTicketResponse;
import com.example.booking_service.exceptions.TicketNotFoundException;
import com.example.booking_service.logging.RequestCorrelationFilter;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.model.TicketStatus;
import com.example.booking_service.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public List<Ticket> getTicketsForBooking(UUID bookingId) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        long startTime = System.currentTimeMillis();
        log.info("requestId={} ticket service getTicketsForBooking started bookingId={}", requestId, bookingId);

        List<Ticket> tickets = ticketRepository.findAllByBookingIdOrderByIssuedAtAsc(bookingId).stream()
                .toList();
        log.info("requestId={} ticket service getTicketsForBooking completed bookingId={} tickets={} latencyMs={}",
                requestId, bookingId, tickets.size(), System.currentTimeMillis() - startTime);
        return tickets;
    }

    @Transactional(readOnly = true)
    public Ticket getTicket(UUID ticketId) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        long startTime = System.currentTimeMillis();
        log.info("requestId={} ticket service getTicket started ticketId={}", requestId, ticketId);

        try {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new TicketNotFoundException(ticketId));
            log.info("requestId={} ticket service getTicket completed ticketId={} latencyMs={}",
                    requestId, ticketId, System.currentTimeMillis() - startTime);
            return ticket;
        } catch (TicketNotFoundException ex) {
            log.warn("requestId={} ticket service getTicket failed ticketId={} latencyMs={} reason={}",
                    requestId, ticketId, System.currentTimeMillis() - startTime, ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public Ticket scanTicket(ScanTicketRequest request) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        long startTime = System.currentTimeMillis();
        log.info("requestId={} ticket service scanTicket started ticketCode={}", requestId, request.ticketCode());

        try {
            Ticket ticket = ticketRepository.findByTicketCode(request.ticketCode())
                    .orElseThrow(() -> new TicketNotFoundException(request.ticketCode()));
            ticket.setStatus(TicketStatus.USED);
            ticket.setCheckedInAt(OffsetDateTime.now());
            Ticket savedTicket = ticketRepository.save(ticket);
            log.info("requestId={} ticket service scanTicket completed ticketId={} status={} latencyMs={}",
                    requestId, savedTicket.getId(), savedTicket.getStatus(), System.currentTimeMillis() - startTime);
            return savedTicket;
        } catch (TicketNotFoundException ex) {
            log.warn("requestId={} ticket service scanTicket failed ticketCode={} latencyMs={} reason={}",
                    requestId, request.ticketCode(), System.currentTimeMillis() - startTime, ex.getMessage());
            throw ex;
        }
    }
}
