package ru.practicum.ewm.main.dto.updateRequests;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@AllArgsConstructor
@Builder
@Jacksonized
public class UpdateCommentUserRequest {
    @Size(max = 2000, message = "количество символов поля 'text' - 2000.")
    private String text;
}
