package ru.practicum.ewm.main.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.practicum.ewm.main.enums.RequestStatus;

import java.util.List;

@Getter
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {
    @NotEmpty(message = "Список ids не может быть пустым")
    private List<Long> requestIds;

    @NotNull(message = "Поле 'status' не должно быть null.")
    private RequestStatus status;
}
