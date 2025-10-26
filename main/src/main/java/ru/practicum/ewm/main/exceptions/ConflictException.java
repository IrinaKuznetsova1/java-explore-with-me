package ru.practicum.ewm.main.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ConflictException extends RuntimeException {
    private final String reason;
    private final LocalDateTime timestamp = LocalDateTime.now();

    public ConflictException(String message, String reason) {
        super(message);
        this.reason  = reason;
    }
}
