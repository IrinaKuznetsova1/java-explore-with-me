package ru.practicum.ewm.main.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.newRequests.NewComment;
import ru.practicum.ewm.main.dto.responses.CommentDto;
import ru.practicum.ewm.main.dto.updateRequests.UpdateCommentUserRequest;
import ru.practicum.ewm.main.services.CommentService;

import java.util.Collection;

@RestController
@RequestMapping(path = "/users/{userId}/comments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PrivateCommentController {
    private final CommentService commentService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CommentDto createComment(@PathVariable @Min(1) long userId,
                                    @RequestParam @Min(1) long eventId,
                                    @Valid @RequestBody @NotNull NewComment newComment) {
        log.info("Получен запрос POST/users/{}/comments?eventId={}.", userId, eventId);
        return commentService.createComment(userId, eventId, newComment);
    }

    @PatchMapping("/{comId}")
    public CommentDto updateComment(@PathVariable @Min(1) long userId,
                                    @PathVariable @Min(1) long comId,
                                    @Valid @RequestBody @NotNull UpdateCommentUserRequest request) {
        log.info("Получен запрос PATCH/users/{}/comments/{}.", userId, comId);
        return commentService.updateCommentByUser(userId, comId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{comId}")
    public void deleteComment(@PathVariable @Min(1) long userId,
                              @PathVariable @Min(1) long comId) {
        log.info("Получен запрос DELETE/users/{}/comments/{}.", userId, comId);
        commentService.deleteComment(userId, comId);
    }

    @GetMapping("/{comId}")
    public CommentDto findComment(@PathVariable @Min(1) long userId,
                                  @PathVariable @Min(1) long comId) {
        log.info("Получен запрос GET/users/{}/comments/{}.", userId, comId);
        return commentService.findCommentByIdUser(userId, comId);
    }

    @GetMapping
    public Collection<CommentDto> findCommentsByAuthor(@PathVariable @Min(1) long userId,
                                                       @RequestParam(defaultValue = "0") @Min(0) int from,
                                                       @RequestParam(defaultValue = "10") @Min(1) int size) {
        log.info("Получен запрос GET/users/{}/comments", userId);
        return commentService.findCommentsByAuthor(userId, from, size);
    }

    @PatchMapping("/{comId}/addLike")
    public CommentDto addLike(@PathVariable @Min(1) long userId,
                              @PathVariable @Min(1) long comId) {
        log.info("Получен запрос PATCH/users/{}/comments/{}/addLike", userId, comId);
        return commentService.addLikeToComment(userId, comId);
    }

    @PatchMapping("/{comId}/deleteLike")
    public CommentDto deleteLike(@PathVariable @Min(1) long userId,
                                 @PathVariable @Min(1) long comId) {
        log.info("Получен запрос PATCH/users/{}/comments/{}/deleteLike", userId, comId);
        return commentService.deleteLikeFromComment(userId, comId);
    }

    @PatchMapping("/{comId}/addDislike")
    public CommentDto addDislike(@PathVariable @Min(1) long userId,
                                 @PathVariable @Min(1) long comId) {
        log.info("Получен запрос PATCH/users/{}/comments/{}/addDislike", userId, comId);
        return commentService.addDislikeToComment(userId, comId);
    }

    @PatchMapping("/{comId}/deleteDislike")
    public CommentDto deleteDislike(@PathVariable @Min(1) long userId,
                                    @PathVariable @Min(1) long comId) {
        log.info("Получен запрос PATCH/users/{}/comments/{}/deleteDislike", userId, comId);
        return commentService.deleteDislikeFromComment(userId, comId);
    }
}
