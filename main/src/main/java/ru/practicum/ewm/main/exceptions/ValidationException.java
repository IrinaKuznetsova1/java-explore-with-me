package ru.practicum.ewm.main.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ValidationException extends RuntimeException {
    private final String reason;
    private final LocalDateTime timestamp = LocalDateTime.now();

    public ValidationException(String message, String reason) {
        super(message);
        this.reason = reason;
    }
}
