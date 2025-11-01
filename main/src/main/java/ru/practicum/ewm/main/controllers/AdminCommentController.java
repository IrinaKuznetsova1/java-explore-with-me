package ru.practicum.ewm.main.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.responses.CommentDto;
import ru.practicum.ewm.main.dto.updateRequests.UpdateCommentAdminRequest;
import ru.practicum.ewm.main.services.CommentService;

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
}
