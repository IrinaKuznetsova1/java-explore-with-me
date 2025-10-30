package ru.practicum.ewm.main.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import ru.practicum.ewm.main.enums.RequestStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ParticipationRequestDto {
    private long id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created;
    private long event;
    private long requester;
    private RequestStatus status;
}
