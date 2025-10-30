package ru.practicum.ewm.main.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import ru.practicum.ewm.main.model.Location;

import java.time.LocalDateTime;

@Builder
@Jacksonized
@Getter
public class NewEventDto {
    @Positive(message = "поле 'category' должно быть указано и быть больше нуля.")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private int category;

    @NotBlank(message = "поле 'title' не должно быть null или быть пустым.")
    @Size(min = 3, max = 120, message = "количество символов поля 'title' - от 3 до 120.")
    private String title;

    @NotBlank(message = "поле 'annotation' не должно быть null или быть пустым.")
    @Size(min = 20, max = 2000, message = "количество символов поля 'annotation' - от 20 до 2000.")
    private String annotation;

    @NotBlank(message = "поле 'description' не должно быть null или быть пустым.")
    @Size(min = 20, max = 7000, message = "количество символов поля 'description' - от 20 до 7000.")
    private String description;

    @NotNull(message = "поле 'eventDate' не должно быть null.")
    @Future(message = "дата 'eventDate' не должна быть прошедшей.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @NotNull(message = "поле 'location' не должно быть null.")
    private Location location;

    @JsonFormat(shape = JsonFormat.Shape.BOOLEAN)
    private boolean paid;

    @JsonFormat(shape = JsonFormat.Shape.BOOLEAN)
    @Builder.Default
    private boolean requestModeration = true;

    @PositiveOrZero(message = "поле 'participantLimit' должно быть больше нуля или равно 0.")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    @Builder.Default
    private int participantLimit = 0;
}
