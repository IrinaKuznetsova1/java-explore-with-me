package ru.practicum.ewm.main.controllers;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.responses.CommentDto;
import ru.practicum.ewm.main.enums.CommentSort;
import ru.practicum.ewm.main.services.CommentService;

import java.util.Collection;

@RestController
@RequestMapping(path = "/events/{eventId}/comments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PublicCommentController {
    private final CommentService commentService;

    @GetMapping
    public Collection<CommentDto> findComments(
            @PathVariable @Min(1) long eventId,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "COMMENT_DATE") CommentSort sort) {
        log.info("Получен запрос GET/events/{}/comments", eventId);
        return commentService.findCommentsPublic(eventId, from, size, sort);
    }

    @GetMapping("/{comId}")
    public CommentDto findCommentById(@PathVariable @Min(1) long eventId, @PathVariable @Min(1) long comId) {
        log.info("Получен запрос GET/events/{}/comments/{}.", eventId, comId);
        return commentService.findCommentByIdPublic(eventId, comId);
    }
}
