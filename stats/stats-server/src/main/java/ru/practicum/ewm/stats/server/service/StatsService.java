package ru.practicum.ewm.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.EndpointHitNewRequest;
import ru.practicum.ewm.stats.dto.ViewStats;
import ru.practicum.ewm.stats.server.repository.StatsRepository;
import ru.practicum.ewm.stats.server.exceptions.TimeValidationException;
import ru.practicum.ewm.stats.server.mapper.StatsMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {
    private final StatsRepository repository;
    private final StatsMapper statsMapper;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void create(EndpointHitNewRequest endpointHitNewRequest) {
        if (endpointHitNewRequest == null) {
            log.warn("Невозможно сохранить в статистику просмотр, который равен null.");
            throw new IllegalArgumentException("Невозможно сохранить в статистику endpointHitNewRequest, который равен null.");
        }
        repository.save(statsMapper.toEndpointHit(endpointHitNewRequest));
    }

    public List<ViewStats> getStats(String start, String end, List<String> uris, boolean unique) {
        LocalDateTime startDate = LocalDateTime.parse(start, dtf);
        LocalDateTime endDate = LocalDateTime.parse(end, dtf);

        if (endDate.isBefore(startDate)) {
            log.warn("Получить статистику невозможно: дата end не может быть раньше даты start.");
            throw new TimeValidationException("end", "Дата end не может быть раньше даты start.");
        }

        if (unique) {
            log.info("Поиск статистики c уникальными ip.");
            return repository.findUniqueViewStats(startDate, endDate, uris);
        } else {
            log.info("Поиск статистики c неуникальными ip.");
            return repository.findAllViewStats(startDate, endDate, uris);
        }
    }
}
