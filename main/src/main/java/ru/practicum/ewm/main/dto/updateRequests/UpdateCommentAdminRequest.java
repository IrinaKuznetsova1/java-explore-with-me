package ru.practicum.ewm.main.dto.updateRequests;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import ru.practicum.ewm.main.enums.CommentState;

@Data
@AllArgsConstructor
public class UpdateCommentAdminRequest {
    @Size(max = 2000, message = "количество символов поля 'text' - 2000.")
    private String text;

    private CommentState state;
}
