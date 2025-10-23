package ru.practicum.ewm.stats.dto;

import lombok.Data;

@Data
public class ViewStats {
    private final String app;
    private final String uri;
    private final long hits;
}
