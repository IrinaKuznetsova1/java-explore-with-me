package ru.practicum.ewm.main.dto.updateRequests;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import ru.practicum.ewm.main.enums.CommentState;

@Data
@AllArgsConstructor
@Builder
@Jacksonized
public class UpdateCommentAdminRequest {
    @Size(max = 2000, message = "количество символов поля 'text' - 2000.")
    private String text;

    private CommentState state;
}
