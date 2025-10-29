package ru.practicum.ewm.main.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TimeValidationException extends RuntimeException {
    private final String reason;
    private final LocalDateTime timestamp = LocalDateTime.now();

    public TimeValidationException(String reason, String message) {
        super(message);
        this.reason = reason;
    }
}
