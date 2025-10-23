package ru.practicum.ewm.stats.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EndpointHitDto {
    private final long id;
    private final String app;
    private final String uri;
    private final String ip;
    private final LocalDateTime timestamp;
}
