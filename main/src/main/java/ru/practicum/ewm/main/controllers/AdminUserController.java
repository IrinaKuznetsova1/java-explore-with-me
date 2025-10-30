package ru.practicum.ewm.main.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.NewUserRequest;
import ru.practicum.ewm.main.dto.UserDto;
import ru.practicum.ewm.main.services.UserService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping(path = "/admin/users")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminUserController {
    private final UserService service;

    @GetMapping
    public Collection<UserDto> findUsers(@RequestParam(required = false) List<Long> ids,
                                         @RequestParam(defaultValue = "0") @Min(0) int from,
                                         @RequestParam(defaultValue = "10") @Min(1) int size) {
        log.info("Получен запрос GET/admin/users?ids={}&from={}&size={}", ids, from, size);
        return service.findUsers(ids, from, size);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public UserDto create(@Valid @RequestBody NewUserRequest newUser) {
        log.info("Получен запрос POST/admin/users.");
        return service.create(newUser);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable @Min(1) long userId) {
        log.info("Получен запрос DELETE/admin/users/{}", userId);
        service.deleteUser(userId);
    }
}
