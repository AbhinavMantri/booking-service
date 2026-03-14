package com.example.booking_service.dtos.common;

import lombok.Data;

@Data
public class ApiResponse {
    private String message;
    private ResponseStatus status;
}
