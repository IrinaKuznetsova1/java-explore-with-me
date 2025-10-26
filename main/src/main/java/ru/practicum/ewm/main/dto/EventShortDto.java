package ru.practicum.ewm.main.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import ru.practicum.ewm.main.model.Category;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EventShortDto {
    private long id;
    private Category category;
    private String title;
    private String annotation;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;
    private UserShortDto initiator;
    private boolean paid;
    private int confirmedRequests;
    private long views;
}
