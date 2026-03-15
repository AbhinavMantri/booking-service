package com.example.booking_service.controllers;

import com.example.booking_service.dtos.ScanTicketRequest;
import com.example.booking_service.dtos.ScanTicketResponse;
import com.example.booking_service.dtos.TicketDetails;
import com.example.booking_service.dtos.TicketResponse;
import com.example.booking_service.dtos.common.ResponseStatus;
import com.example.booking_service.exceptions.TicketNotFoundException;
import com.example.booking_service.logging.RequestCorrelationFilter;
import com.example.booking_service.model.Ticket;
import com.example.booking_service.services.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService ticketService;

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable UUID ticketId) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        long startTime = System.currentTimeMillis();
        log.info("requestId={} get ticket request received ticketId={}", requestId, ticketId);

        TicketResponse response = new TicketResponse();
        try {
            Ticket ticket = ticketService.getTicket(ticketId);
            response.setTicketDetails(to(ticket));
            response.setMessage("Ticket reterieved successfully");
            response.setStatus(ResponseStatus.SUCCESS);
            log.info("requestId={} get ticket request completed ticketId={} latencyMs={}",
                    requestId, ticketId, System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);
        } catch (TicketNotFoundException e) {
            response.setMessage(e.getMessage());
            response.setStatus(ResponseStatus.FAILURE);
            log.warn("requestId={} get ticket request failed ticketId={} latencyMs={} reason={}",
                    requestId, ticketId, System.currentTimeMillis() - startTime, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping("/scan")
    public ResponseEntity<ScanTicketResponse> scanTicket(@Valid @RequestBody ScanTicketRequest request) {
        String requestId = RequestCorrelationFilter.getCurrentRequestId();
        long startTime = System.currentTimeMillis();
        log.info("requestId={} scan ticket request received ticketCode={}", requestId, request.ticketCode());

        ScanTicketResponse response = new ScanTicketResponse();
        try {
            Ticket ticket = ticketService.scanTicket(request);
            response.setTicketId(ticket.getId());
            response.setTicketStatus(ticket.getStatus());
            response.setMessage("Ticket scanned successfully");
            response.setStatus(ResponseStatus.SUCCESS);
            log.info("requestId={} scan ticket request completed ticketId={} status={} latencyMs={}",
                    requestId, ticket.getId(), ticket.getStatus(), System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(response);
        } catch (TicketNotFoundException e) {
            response.setMessage(e.getMessage());
            response.setStatus(ResponseStatus.FAILURE);
            log.warn("requestId={} scan ticket request failed ticketCode={} latencyMs={} reason={}",
                    requestId, request.ticketCode(), System.currentTimeMillis() - startTime, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    private TicketDetails to(Ticket ticket) {
        return TicketDetails.builder()
                .ticketId(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .ticketStatus(ticket.getStatus())
                .ticketCode(ticket.getTicketCode())
                .build();
    }
}
