package ru.practicum.ewm.main.controllers;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.services.RequestService;

import java.util.Collection;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PrivateRequestController {
    private final RequestService requestService;

    @GetMapping
    public Collection<ParticipationRequestDto> findUsersRequests(@PathVariable @Min(1) long userId) {
        log.info("Получен запрос GET/users/{}/requests.", userId);
        return requestService.findUsersRequests(userId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ParticipationRequestDto createEvent(@PathVariable @Min(1) long userId,
                                               @RequestParam @Min(1) long eventId) {

        log.info("Получен запрос POST/users/{}/requests?eventId={}.", userId, eventId);
        return requestService.create(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable @Min(1) long userId,
                                                 @PathVariable @Min(1) long requestId) {
        log.info("Получен запрос PATCH/users/{}/requests/{}.", userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }
}
