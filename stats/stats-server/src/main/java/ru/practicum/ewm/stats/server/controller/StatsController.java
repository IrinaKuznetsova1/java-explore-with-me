package ru.practicum.ewm.stats.server.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.EndpointHitNewRequest;
import ru.practicum.ewm.stats.dto.ViewStats;
import ru.practicum.ewm.stats.server.service.StatsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatsController {
    private final StatsService service;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/hit")
    public void create(@RequestBody EndpointHitNewRequest endpointHitNewRequest) {
        log.info("Получен запрос POST/hit");
        service.create(endpointHitNewRequest);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(@RequestParam @NonNull String start,
                                    @RequestParam @NonNull String end,
                                    @RequestParam(required = false) List<String> uris,
                                    @RequestParam(defaultValue = "false") boolean unique) {
        log.info("Получен запрос GET/stats");
        return service.getStats(start, end, uris, unique);
    }
}
