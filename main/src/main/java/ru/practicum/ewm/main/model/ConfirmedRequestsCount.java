package ru.practicum.ewm.main.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfirmedRequestsCount {
    private long eventId;
    private long count;
}
