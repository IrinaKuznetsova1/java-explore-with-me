package ru.practicum.ewm.main.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.*;
import ru.practicum.ewm.main.model.Request;
import ru.practicum.ewm.main.services.EventService;
import ru.practicum.ewm.main.services.RequestService;

import java.util.Collection;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PrivateEventController {
    private final EventService eventService;
    private final RequestService requestService;

    @GetMapping
    public Collection<EventShortDto> findEventsByUserId(@PathVariable long userId,
                                                        @RequestParam(defaultValue = "0") @Min(0) int from,
                                                        @RequestParam(defaultValue = "10") @Min(1) int size) {
        return eventService.findEventsByUserId(userId, from, size);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public EventFullDto createEvent(@PathVariable @Min(1) long userId,
                                    @Valid @RequestBody @NotNull NewEventDto newEvent) {
        return eventService.createEvent(userId, newEvent);
    }

    @GetMapping("/{eventId}")
    public EventFullDto findEventByIdAndUserId(@PathVariable long eventId,
                                               @PathVariable long userId) {
        return eventService.findEventByIdAndUserId(eventId, userId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByUser(@Valid @RequestBody UpdateEventUserRequest request,
                                          @PathVariable long eventId,
                                          @PathVariable long userId) {
        return eventService.updateEventByUser(request, eventId, userId);
    }

    @GetMapping("/{eventId}/requests")
    public Collection<ParticipationRequestDto> findRequests(@PathVariable long eventId,
                                                            @PathVariable long userId) {
        return requestService.findRequests(eventId, userId);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestsStatus(@PathVariable long eventId,
                                                               @PathVariable long userId,
                                                               @Valid @RequestBody @NotNull EventRequestStatusUpdateRequest request) {
        return requestService.updateRequestsStatus(eventId, userId, request);
    }
}
