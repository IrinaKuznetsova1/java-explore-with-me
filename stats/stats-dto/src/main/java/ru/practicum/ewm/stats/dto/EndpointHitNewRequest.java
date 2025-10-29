package ru.practicum.ewm.stats.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Builder
@Jacksonized
@Getter
public class EndpointHitNewRequest {
    @NotBlank(message = "поле 'app' не должно быть null или быть пустым.")
    @Size(max = 32, message = "максимальная длина поля 'app' - 32 символа.")
    private final String app;

    @NotBlank(message = "поле 'uri' не должно быть null или быть пустым.")
    @Size(max = 128, message = "максимальная длина поля 'uri' - 128 символов.")
    private final String uri;

    @NotBlank(message = "поле 'ip' не должно быть null или быть пустым.")
    @Size(max = 16, message = "максимальная длина поля 'ip' - 16 символов.")
    private final String ip;

    @NotNull(message = "Дата 'timestamp' должна быть указана.")
    @PastOrPresent(message = "Дата 'timestamp' не быть быть в будущем.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
