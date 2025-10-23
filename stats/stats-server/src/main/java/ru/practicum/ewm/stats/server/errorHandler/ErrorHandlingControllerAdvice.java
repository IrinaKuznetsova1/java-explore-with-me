package ru.practicum.ewm.stats.server.errorHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.ewm.stats.server.exceptions.TimeValidationException;


@Slf4j
@RestControllerAdvice
public class ErrorHandlingControllerAdvice {
    @ExceptionHandler(TimeValidationException.class)
    public ResponseEntity<Violation> onTimeValidationException(TimeValidationException e) {
        log.warn("Обработка исключения TimeValidationException: {}", e.getMessage());
        return new ResponseEntity<>(new Violation(e.getFieldNameWithError(), e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> onIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Обработка исключения IllegalArgumentException: {}", e.getMessage());
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
