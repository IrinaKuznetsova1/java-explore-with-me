package ru.practicum.ewm.main.dto.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import ru.practicum.ewm.main.enums.CommentState;
import ru.practicum.ewm.main.model.User;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
public class CommentDto {
    private long id;
    private long eventId;
    private long authorId;
    private String text;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created;
    private CommentState state;
    private long useful;
    private Set<User> likes;
    private Set<User> dislikes;
}
