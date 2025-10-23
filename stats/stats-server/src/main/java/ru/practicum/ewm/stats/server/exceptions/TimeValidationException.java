package ru.practicum.ewm.stats.server.exceptions;

import lombok.Getter;

@Getter
public class TimeValidationException extends RuntimeException {
    private final String fieldNameWithError;

    public TimeValidationException(String fieldNameWithError, String message) {
        super(message);
        this.fieldNameWithError = fieldNameWithError;
    }
}
