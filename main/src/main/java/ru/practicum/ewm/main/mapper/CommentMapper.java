package ru.practicum.ewm.main.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.main.dto.newRequests.NewComment;
import ru.practicum.ewm.main.dto.responses.CommentDto;
import ru.practicum.ewm.main.dto.updateRequests.UpdateCommentAdminRequest;
import ru.practicum.ewm.main.dto.updateRequests.UpdateCommentUserRequest;
import ru.practicum.ewm.main.model.Comment;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", source = "event")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "created", expression = "java(getNow())")
    @Mapping(target = "state", expression = "java(ru.practicum.ewm.main.enums.CommentState.PENDING)")
    Comment toComment(NewComment newComment, Event event, User author);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Comment updateCommentUser(UpdateCommentUserRequest request, @MappingTarget Comment comment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Comment updateCommentAdmin(UpdateCommentAdminRequest request, @MappingTarget Comment comment);

    @Mapping(target = "eventId", source = "comment.event.id")
    @Mapping(target = "authorId", source = "comment.author.id")
    @Mapping(target = "useful", expression = "java(getUseful(comment))")
    CommentDto toCommentDto(Comment comment);

    List<CommentDto> toCommentDtoList(List<Comment> comments);

    default LocalDateTime getNow() {
        return LocalDateTime.now();
    }

    default long getUseful(Comment comment) {
        return (comment.getLikes().size() - comment.getDislikes().size());
    }
}
