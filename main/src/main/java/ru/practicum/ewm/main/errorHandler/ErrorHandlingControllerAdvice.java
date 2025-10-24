package ru.practicum.ewm.main.errorHandler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.ewm.main.exceptions.DuplicatedDataException;
import ru.practicum.ewm.main.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandlingControllerAdvice {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> onNotFoundException(NotFoundException e) {
        log.warn("Обработка исключения NotFoundException: {}", e.getMessage());
        return new ResponseEntity<>(new ApiError(
                e.getMessage(),
                e.getReason(),
                HttpStatus.NOT_FOUND.toString(),
                e.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicatedDataException.class)
    public ResponseEntity<ApiError> onDuplicatedDataException(DuplicatedDataException e) {
        log.warn("Обработка исключения DuplicatedDataException: {}", e.getMessage());
        return new ResponseEntity<>(new ApiError(
                e.getMessage(),
                e.getReason(),
                HttpStatus.CONFLICT.toString(),
                e.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> onConstraintValidationException(ConstraintViolationException e) {
        log.warn("Обработка исключения ConstraintViolationException: {}", e.getMessage());
        final ApiError apiError = new ApiError(
                e.getMessage(),
                e.getCause().toString(),
                HttpStatus.BAD_REQUEST.toString(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        apiError.setErrors(e.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList()));
        return new ResponseEntity<> (apiError, HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Обработка исключения MethodArgumentNotValidException: {}", e.getMessage());
        final ApiError apiError = new ApiError(
                e.getMessage(),
                "Неправильно составленный запрос.",
                HttpStatus.BAD_REQUEST.toString(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        apiError.setErrors(e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList()));
        return new ResponseEntity<> (apiError, HttpStatus.BAD_REQUEST);
    }
}
