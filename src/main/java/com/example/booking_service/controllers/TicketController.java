package com.example.booking_service.controllers;

import com.example.booking_service.dtos.ScanTicketRequest;
import com.example.booking_service.dtos.ScanTicketResponse;
import com.example.booking_service.dtos.TicketResponse;
import com.example.booking_service.services.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tickets")
public class TicketController {
    private final TicketService ticketService;

    @GetMapping("/{ticketId}")
    public TicketResponse getTicket(@PathVariable UUID ticketId) {
        return ticketService.getTicket(ticketId);
    }

    @PostMapping("/scan")
    public ScanTicketResponse scanTicket(@Valid @RequestBody ScanTicketRequest request) {
        return ticketService.scanTicket(request);
    }
}
