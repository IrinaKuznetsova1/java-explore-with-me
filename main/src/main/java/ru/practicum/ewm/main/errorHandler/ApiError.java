package ru.practicum.ewm.main.errorHandler;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ApiError {
    private List<String> errors = new ArrayList<>();
    private final String message;
    private final String reason;
    private final String status;
    private final String timestamp;
}
