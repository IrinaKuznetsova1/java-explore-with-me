package ru.practicum.ewm.main.exceptions;

import java.time.LocalDateTime;

public class NotAvailableException extends RuntimeException {
    private final String reason;
    private final LocalDateTime timestamp = LocalDateTime.now();

    public NotAvailableException(String message, String reason) {
        super(message);
        this.reason = reason;
    }
}
