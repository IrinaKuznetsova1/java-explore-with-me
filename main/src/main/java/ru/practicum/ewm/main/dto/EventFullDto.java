package ru.practicum.ewm.main.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import ru.practicum.ewm.main.enums.EventsState;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.Location;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EventFullDto {
    private long id;
    private Category category;

    private String title;
    private String annotation;
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdOn;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedOn;

    private UserShortDto initiator;
    private Location location;
    private EventsState state;

    private boolean paid;
    private boolean requestModeration;

    private long confirmedRequests;
    private long participantLimit;
    private long views;
}
