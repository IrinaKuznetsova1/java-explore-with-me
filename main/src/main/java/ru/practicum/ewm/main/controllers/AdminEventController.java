package ru.practicum.ewm.main.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.EventFullDto;
import ru.practicum.ewm.main.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.main.enums.EventsState;
import ru.practicum.ewm.main.services.EventService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminEventController {
    private final EventService eventService;

    @GetMapping
    public Collection<EventFullDto> findEventsByAdmin(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<EventsState> states,
            @RequestParam(required = false) List<Integer> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        log.info("Получен запрос GET/admin/events.");
        return eventService.findEventsByAdmin(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByAdmin(@Valid @RequestBody @NotNull UpdateEventAdminRequest request,
                                           @PathVariable @Min(1) long eventId) {
        log.info("Получен запрос PATCH/admin/events/{}.", eventId);
        return eventService.updateEventByAdmin(request, eventId);
    }
}
