package ru.practicum.ewm.main.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import ru.practicum.ewm.main.annotation.NoSpaces;
import ru.practicum.ewm.main.enums.StateActionUser;
import ru.practicum.ewm.main.model.Location;

import java.time.LocalDateTime;

@Builder
@Jacksonized
@Getter
public class UpdateEventUserRequest {
    @Positive(message = "поле 'category' должно быть больше нуля.")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private Integer category;

    @NoSpaces(message = "поле 'title' не должно быть пустым.")
    @Size(min = 3, max = 120, message = "количество символов поля 'title' - от 3 до 120.")
    private String title;

    @NoSpaces(message = "поле 'annotation' не должно быть пустым.")
    @Size(min = 20, max = 2000, message = "количество символов поля 'annotation' - от 20 до 2000.")
    private String annotation;

    @NoSpaces(message = "поле 'description' не должно быть пустым.")
    @Size(min = 20, max = 7000, message = "количество символов поля 'description' - от 20 до 7000.")
    private String description;

    @Future(message = "дата 'eventDate' не должна быть прошедшей.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.")
    private LocalDateTime eventDate;

    private Location location;

    @JsonFormat(shape = JsonFormat.Shape.BOOLEAN)
    private Boolean paid;

    @JsonFormat(shape = JsonFormat.Shape.BOOLEAN)
    private Boolean requestModeration = true;

    @Positive(message = "поле 'participantLimit' должно быть больше нуля.")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private Integer participantLimit;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private StateActionUser stateAction;
}
