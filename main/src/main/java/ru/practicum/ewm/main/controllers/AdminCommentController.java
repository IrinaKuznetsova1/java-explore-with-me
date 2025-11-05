package ru.practicum.ewm.main.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.responses.CommentDto;
import ru.practicum.ewm.main.dto.updateRequests.UpdateCommentAdminRequest;
import ru.practicum.ewm.main.enums.CommentState;
import ru.practicum.ewm.main.services.CommentService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping(path = "/admin/comments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCommentController {
    private final CommentService commentService;

    @PatchMapping("/{comId}")
    public CommentDto updateComment(@PathVariable @Min(1) long comId,
                                    @Valid @RequestBody @NotNull UpdateCommentAdminRequest request) {
        log.info("Получен запрос PATCH/admin/comments/{}", comId);
        return commentService.updateCommentByAdmin(comId, request);
    }

    @GetMapping
    public Collection<CommentDto> findCommentsByAdmin(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<CommentState> states,
            @RequestParam(required = false) List<Long> events,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        log.info("Получен запрос GET/admin/comments.");
        return commentService.findCommentsByAdmin(users, states, events, rangeStart, rangeEnd, from, size);
    }

    @GetMapping("/{comId}")
    public CommentDto getCommentByAdmin(@PathVariable @Min(1) long comId) {
        log.info("Получен запрос GET/admin/comments/{}.", comId);
        return commentService.findCommentByIdAdmin(comId);
    }
}
